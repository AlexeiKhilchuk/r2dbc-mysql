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

package io.github.mirromutth.r2dbc.mysql;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static io.github.mirromutth.r2dbc.mysql.internal.AssertUtils.requireNonNull;

/**
 * A column metadata bundle for searching metadata by id or column name.
 *
 * @see MySqlColumnNames column name searching rules.
 */
final class CollatedColumnMetadata {

    private static final Comparator<MySqlColumnMetadata> NAME_COMPARATOR = (left, right) ->
        MySqlColumnNames.compare(left.getName(), right.getName());

    private final MySqlColumnMetadata[] idSorted;

    private final MySqlColumnMetadata[] nameSorted;

    /**
     * Copied column names from {@link #nameSorted}.
     */
    private final String[] names;

    private final ColumnNameSet nameSet;

    CollatedColumnMetadata(MySqlColumnMetadata[] idSorted) {
        int size = idSorted.length;

        if (size <= 0) {
            throw new IllegalArgumentException("least 1 column metadata");
        }

        MySqlColumnMetadata[] nameSorted = new MySqlColumnMetadata[size];
        System.arraycopy(idSorted, 0, nameSorted, 0, size);
        Arrays.sort(nameSorted, NAME_COMPARATOR);

        this.idSorted = idSorted;
        this.nameSorted = nameSorted;
        this.names = getNames(nameSorted);
        this.nameSet = ColumnNameSet.fromSorted(this.names);
    }

    @Override
    public String toString() {
        return Arrays.toString(idSorted);
    }

    MySqlColumnMetadata[] getIdSorted() {
        return idSorted;
    }

    MySqlColumnMetadata getMetadata(int index) {
        if (index < 0 || index >= idSorted.length) {
            throw new ArrayIndexOutOfBoundsException(String.format("column index %d is invalid, total %d", index, idSorted.length));
        }

        return idSorted[index];
    }

    MySqlColumnMetadata getMetadata(String name) {
        requireNonNull(name, "name must not be null");

        int index = MySqlColumnNames.nameSearch(this.names, name);

        if (index < 0) {
            throw new NoSuchElementException(String.format("column name '%s' does not exist in %s", name, Arrays.toString(this.names)));
        }

        return nameSorted[index];
    }

    Set<String> nameSet() {
        return nameSet;
    }

    List<MySqlColumnMetadata> allValues() {
        switch (idSorted.length) {
            case 0:
                return Collections.emptyList();
            case 1:
                return Collections.singletonList(idSorted[0]);
            default:
                return Collections.unmodifiableList(Arrays.asList(idSorted));
        }
    }

    private static String[] getNames(MySqlColumnMetadata[] metadata) {
        int size = metadata.length;
        String[] names = new String[size];

        for (int i = 0; i < size; ++i) {
            names[i] = metadata[i].getName();
        }

        return names;
    }
}
