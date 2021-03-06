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
package eu.eexcess.dataformats;

import java.util.LinkedList;
import java.util.Stack;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import eu.eexcess.dataformats.result.ResultStats;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PartnerBadgeStats {
	
	
	@XmlElement(name="requestCount")
	public int requestCount=0;

	@XmlElement(name="failedRequestCount")
	public int failedRequestCount=0;

	@XmlElement(name="failedRequestTimeoutCount")
	public int failedRequestTimeoutCount=0;
	
	@XmlElement(name="lastQueries")
	public LinkedList<ResultStats> lastQueries = new LinkedList<ResultStats>();

	//Begin response time
	@XmlTransient
	public Stack<Long> lastResponseTimes= new Stack<Long>() ;
	@XmlElement(name="shortTimeResponseTimes")
	public Long shortTimeResponseTime;
	
	//End response time	

}
