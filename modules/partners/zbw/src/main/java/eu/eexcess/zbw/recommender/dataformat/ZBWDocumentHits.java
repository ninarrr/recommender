/* Copyright (C) 2014
"Kompetenzzentrum fuer wissensbasierte Anwendungen Forschungs- und EntwicklungsgmbH" 
(Know-Center), Graz, Austria, office@know-center.at.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package eu.eexcess.zbw.recommender.dataformat;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
/**
 * 
 * @author hziak@know-center.at
 *
 */
public class ZBWDocumentHits {

	@XmlElement(name="max_score")
	public Double maxScore;
	@XmlElement(name="total")
	public Integer total;
	@XmlElement(name="hit")
	public ArrayList<ZBWDocumentHit> hit =new ArrayList<ZBWDocumentHit>();
	@Override
	public String toString() {
		return "ZBWDocumentHits [maxScore=" + maxScore + ", total=" + total
				+ ", hit=" + hit + "]";
	}
	
}
