/* Copyright (C) 2010 
"Kompetenzzentrum fuer wissensbasierte Anwendungen Forschungs- und EntwicklungsgmbH" 
(Know-Center), Graz, Austria, office@know-center.at.

Licensees holding valid Know-Center Commercial licenses may use this file in
accordance with the Know-Center Commercial License Agreement provided with 
the Software or, alternatively, in accordance with the terms contained in
a written agreement between Licensees and Know-Center.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package eu.eexcess.domaindetection;

import java.util.Collection;
import java.util.Set;

/**
 * Classifier to detect the domains found in a text.
 * 
 * @author rkern@know-center.at
 */
public abstract class DomainDetector {
    /**
     * Detects the domains of a given text.
     * @param text the text
     * @return never null
     * @throws DomainDetectorException
     */
    public abstract Set<Domain> detect(String text) throws DomainDetectorException;

    /**
     * Returns a random word, which is ambiguous according to the domain set.
     * @param wordsToIgnore TODO
     * @return
     * @throws DomainDetectorException 
     */
    public abstract String drawRandomAmbiguousWord(Set<String> wordsToIgnore) throws DomainDetectorException;

    /**
     * @return
     */
    public abstract Collection<Domain> getAllDomains();
}
