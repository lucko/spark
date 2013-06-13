/*
 * WarmRoast
 * Copyright (C) 2013 Albert Pham <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.sk89q.warmroast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassMapping {
    
    private final String obfuscated;
    private final String actual;
    private final Map<String, List<String>> methods = new HashMap<>();
    
    public ClassMapping(String obfuscated, String actual) {
        this.obfuscated = obfuscated;
        this.actual = actual;
    }

    public String getObfuscated() {
        return obfuscated;
    }
    
    public String getActual() {
        return actual;
    }
    
    public void addMethod(String obfuscated, String actual) {
        List<String> m = methods.get(obfuscated);
        if (m == null) {
            m = new ArrayList<>();
            methods.put(obfuscated, m);
        }
        m.add(actual);
    }
    
    public List<String> mapMethod(String obfuscated) {
        List<String> m = methods.get(obfuscated);
        if (m == null) {
            return new ArrayList<>();
        }
        return m;
    }
    
    @Override
    public String toString() {
        return getObfuscated() + "->" + getActual();
    }

}
