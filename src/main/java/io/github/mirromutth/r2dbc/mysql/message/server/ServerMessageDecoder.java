/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.mirromutth.r2dbc.mysql.message.server;

import io.github.mirromutth.r2dbc.mysql.constant.DataValues;
import io.github.mirromutth.r2dbc.mysql.constant.Envelopes;
import io.github.mirromutth.r2dbc.mysql.constant.Headers;
import io.github.mirromutth.r2dbc.mysql.internal.MySqlSession;
import io.github.mirromutth.r2dbc.mysql.message.header.SequenceIdProvider;
import io.github.mirromutth.r2dbc.mysql.internal.CodecUtils;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import io.r2dbc.spi.R2dbcPermissionDeniedException;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static io.github.mirromutth.r2dbc.mysql.internal.AssertUtils.requireNonNull;

/**
 * Generic message decoder logic.
 */
public final class ServerMessageDecoder {

    private static final ByteBufJoiner JOINER = ByteBufJoiner.wrapped();

    private final List<ByteBuf> parts = new ArrayList<>();

    @Nullable
    public ServerMessage decode(ByteBuf envelope, MySqlSession session, DecodeContext decodeContext, @Nullable SequenceIdProvider.Linkable idProvider) {
        requireNonNull(envelope, "envelope must not be null");
        requireNonNull(session, "session must not be null");

        if (readNotFinish(envelope, idProvider)) {
            return null;
        }

        return decodeMessage(parts, session, decodeContext);
    }

    public void dispose() {
        try {
            for (ByteBuf part : parts) {
                ReferenceCountUtil.safeRelease(part);
            }
        } finally {
            parts.clear();
        }
    }

    @Nullable
    private static ServerMessage decodeMessage(List<ByteBuf> buffers, MySqlSession session, DecodeContext context) {
        if (context instanceof ResultDecodeContext) {
            // Maybe very large.
            return decodeResult(buffers, session, (ResultDecodeContext) context);
        }

        ByteBuf joined = JOINER.join(buffers);

        try {
            if (context instanceof PreparedMetadataDecodeContext) {
                return decodePreparedMetadata(joined, session, (PreparedMetadataDecodeContext) context);
            } else if (context instanceof WaitPrepareDecodeContext) {
                return decodeOnWaitPrepare(joined, session);
            } else if (context instanceof CommandDecodeContext) {
                return decodeCommandMessage(joined, session);
            } else if (context instanceof ConnectionDecodeContext) {
                return decodeConnectionMessage(joined, session);
            }
        } finally {
            joined.release();
        }

        throw new IllegalStateException("unknown decode context type: " + context.getClass());
    }

    @Nullable
    private static ServerMessage decodePreparedMetadata(ByteBuf buf, MySqlSession session, PreparedMetadataDecodeContext context) {
        short header = buf.getUnsignedByte(buf.readerIndex());

        if (header == Headers.ERROR) {
            // 0xFF is not header of var integer,
            // not header of text result null (0xFB) and
            // not header of column metadata (0x03 + "def")
            return ErrorMessage.decode(buf);
        }

        if (context.isInMetadata()) {
            return decodeInMetadata(buf, header, session, context);
        }

        throw new R2dbcNonTransientResourceException(String.format("Unknown message header 0x%x and readable bytes is %d on prepared metadata phase", header, buf.readableBytes()));
    }

    @Nullable
    private static ServerMessage decodeResult(List<ByteBuf> buffers, MySqlSession session, ResultDecodeContext context) {
        ByteBuf firstBuf = buffers.get(0);
        short header = firstBuf.getUnsignedByte(firstBuf.readerIndex());

        if (Headers.ERROR == header) {
            // 0xFF is not header of var integer,
            // not header of text result null (0xFB) and
            // not header of column metadata (0x03 + "def")
            ByteBuf joined = JOINER.join(buffers);
            try {
                return ErrorMessage.decode(joined);
            } finally {
                joined.release();
            }
        }

        if (context.isInMetadata()) {
            ByteBuf joined = JOINER.join(buffers);
            try {
                return decodeInMetadata(joined, header, session, context);
            } finally {
                joined.release();
            }
            // Should not has other messages when metadata reading.
        }

        if (context.isBinary()) {
            if (Headers.OK == header) {
                // If header is 0, SHOULD NOT be OK message.
                // Binary row message always starts with 0x00 (i.e. binary row header).
                // Because MySQL server sends OK messages always starting with 0xFE in SELECT statement result.
                try (FieldReader reader = FieldReader.of(JOINER, buffers)) {
                    return BinaryRowMessage.decode(reader, context);
                }
            }
        } else if (isTextRow(buffers, firstBuf, header)) {
            try (FieldReader reader = FieldReader.of(JOINER, buffers)) {
                return TextRowMessage.decode(reader, context.getTotalColumns());
            }
        }

        switch (header) {
            case Headers.OK:
                if (OkMessage.isValidSize(firstBuf.readableBytes())) {
                    ByteBuf joined = JOINER.join(buffers);

                    try {
                        return OkMessage.decode(joined, session);
                    } finally {
                        joined.release();
                    }
                }

                break;
            case Headers.EOF:
                int byteSize = firstBuf.readableBytes();

                if (OkMessage.isValidSize(byteSize)) {
                    ByteBuf joined = JOINER.join(buffers);

                    try {
                        return OkMessage.decode(joined, session);
                    } finally {
                        joined.release();
                    }
                } else if (AbstractEofMessage.isValidSize(byteSize)) {
                    ByteBuf joined = JOINER.join(buffers);

                    try {
                        return AbstractEofMessage.decode(joined);
                    } finally {
                        joined.release();
                    }
                }
        }

        long totalBytes = 0;
        try {
            for (ByteBuf buffer : buffers) {
                totalBytes += buffer.readableBytes();
                ReferenceCountUtil.safeRelease(buffer);
            }
        } finally {
            buffers.clear();
        }

        throw new R2dbcNonTransientResourceException(String.format("Unknown message header 0x%x and readable bytes is %d on %s result phase", header, totalBytes, context.isBinary() ? "binary" : "text"));
    }

    private static ServerMessage decodeCommandMessage(ByteBuf buf, MySqlSession session) {
        short header = buf.getUnsignedByte(buf.readerIndex());
        switch (header) {
            case Headers.ERROR:
                return ErrorMessage.decode(buf);
            case Headers.OK:
                if (OkMessage.isValidSize(buf.readableBytes())) {
                    return OkMessage.decode(buf, session);
                }

                break;
            case Headers.EOF:
                int byteSize = buf.readableBytes();

                // Maybe OK, maybe column count (unsupported EOF on command phase)
                if (OkMessage.isValidSize(byteSize)) {
                    // MySQL has hard limit of 4096 columns per-table,
                    // so if readable bytes upper than 7, it means if it is column count,
                    // column count is already upper than (1 << 24) - 1 = 16777215, it is impossible.
                    // So it must be OK message, not be column count.
                    return OkMessage.decode(buf, session);
                } else if (AbstractEofMessage.isValidSize(byteSize)) {
                    return AbstractEofMessage.decode(buf);
                }
        }

        if (CodecUtils.checkNextVarInt(buf) == 0) {
            // EOF message must be 5-bytes, it will never be looks like a var integer.
            // It looks like has only a var integer, should be column count.
            return ColumnCountMessage.decode(buf);
        }

        throw new R2dbcNonTransientResourceException(String.format("Unknown message header 0x%x and readable bytes is %d on command phase", header, buf.readableBytes()));
    }

    private static ServerMessage decodeOnWaitPrepare(ByteBuf buf, MySqlSession session) {
        short header = buf.getUnsignedByte(buf.readerIndex());

        switch (header) {
            case Headers.ERROR:
                return ErrorMessage.decode(buf);
            case Headers.OK:
                // Should be prepared ok, but test in here...
                if (PreparedOkMessage.isLooksLike(buf)) {
                    return PreparedOkMessage.decode(buf);
                } else if (OkMessage.isValidSize(buf.readableBytes())) {
                    return OkMessage.decode(buf, session);
                }

                break;
            case Headers.EOF:
                int byteSize = buf.readableBytes();

                if (OkMessage.isValidSize(byteSize)) {
                    return OkMessage.decode(buf, session);
                } else if (AbstractEofMessage.isValidSize(byteSize)) {
                    return AbstractEofMessage.decode(buf);
                }
        }

        throw new R2dbcNonTransientResourceException(String.format("Unknown message header 0x%x and readable bytes is %d on waiting prepare metadata phase", header, buf.readableBytes()));
    }

    private static ServerMessage decodeConnectionMessage(ByteBuf buf, MySqlSession session) {
        short header = buf.getUnsignedByte(buf.readerIndex());
        switch (header) {
            case Headers.OK:
                if (OkMessage.isValidSize(buf.readableBytes())) {
                    return OkMessage.decode(buf, requireNonNull(session, "session must not be null"));
                }

                break;
            case Headers.AUTH_MORE_DATA: // Auth more data
                return AuthMoreDataMessage.decode(buf);
            case Headers.HANDSHAKE_V9:
            case Headers.HANDSHAKE_V10: // Handshake V9 (not supported) or V10
                return AbstractHandshakeMessage.decode(buf);
            case Headers.ERROR: // Error
                return ErrorMessage.decode(buf);
            case Headers.EOF: // Auth exchange message or EOF message
                if (AbstractEofMessage.isValidSize(buf.readableBytes())) {
                    return AbstractEofMessage.decode(buf);
                } else {
                    return AuthChangeMessage.decode(buf);
                }
        }

        throw new R2dbcPermissionDeniedException(String.format("Unknown message header 0x%x and readable bytes is %d on connection phase", header, buf.readableBytes()));
    }

    @Nullable
    private boolean readNotFinish(ByteBuf envelope, @Nullable SequenceIdProvider.Linkable idProvider) {
        try {
            int size = envelope.readUnsignedMediumLE();
            if (size < Envelopes.MAX_ENVELOPE_SIZE) {
                if (idProvider == null) {
                    // Just ignore sequence id because of no need link any message.
                    envelope.skipBytes(1);
                } else {
                    // Link last message.
                    idProvider.last(envelope.readUnsignedByte());
                }

                parts.add(envelope);
                // success, no need release
                envelope = null;
                return false;
            } else {
                // skip the sequence Id
                envelope.skipBytes(1);
                parts.add(envelope);
                // success, no need release
                envelope = null;
                return true;
            }
        } finally {
            if (envelope != null) {
                envelope.release();
            }
        }
    }

    private static boolean isTextRow(List<ByteBuf> buffers, ByteBuf firstBuf, short header) {
        if (header == DataValues.NULL_VALUE) {
            // NULL_VALUE (0xFB) is not header of var integer and not header of OK (0x0 or 0xFE)
            return true;
        } else if (header == Headers.EOF) {
            // 0xFE means it maybe EOF, or var int (64-bits) header.
            long allBytes = firstBuf.readableBytes();

            if (allBytes > Byte.BYTES + Long.BYTES) {
                long needBytes = firstBuf.getLongLE(firstBuf.readerIndex() + Byte.BYTES) + Byte.BYTES + Long.BYTES;
                // Maybe var int (64-bits), try to get 64-bits var integer.
                // Minimal length for first field with size by var integer encoded.
                // Should not be OK message if it is big message.
                if (allBytes >= needBytes) {
                    return true;
                }

                int size = buffers.size();
                for (int i = 1; i < size; ++i) {
                    allBytes += buffers.get(i).readableBytes();

                    if (allBytes >= needBytes) {
                        return true;
                    }
                }

                return false;
            } else {
                // Is not a var integer, it is not text row.
                return false;
            }
        } else {
            // If header is 0, SHOULD NOT be OK message.
            // Because MySQL server sends OK messages always starting with 0xFE in SELECT statement result.
            // Now, it is not OK message, not be error message, it must be text row.
            return true;
        }
    }

    @Nullable
    private static SyntheticMetadataMessage decodeInMetadata(ByteBuf buf, short header, MySqlSession session, MetadataDecodeContext context) {
        ServerMessage message;

        if (Headers.EOF == header && AbstractEofMessage.isValidSize(buf.readableBytes())) {
            message = AbstractEofMessage.decode(buf);
        } else if (DefinitionMetadataMessage.isLooksLike(buf)) {
            message = DefinitionMetadataMessage.decode(buf, session.getCollation().getCharset());
        } else {
            throw new R2dbcNonTransientResourceException(String.format("Unknown message header 0x%x and readable bytes is %d when reading metadata", header, buf.readableBytes()));
        }

        return context.putPart(message);
    }
}
