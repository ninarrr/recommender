/* Copyright (C) 2014 
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
package eu.eexcess.federatedrecommender.registration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import eu.eexcess.dataformats.PartnerBadge;

/**
 * Registration for the partners PartnerRecommenderApis
 * @author hziak
 *
 */
@XmlRootElement(name = "eexcess-registered-partners")
public class PartnerRegister {
    
    @XmlElement(name="partners")
	private List<PartnerBadge> partners = new ArrayList<PartnerBadge>();
	private Map<PartnerBadge, Client> partnerToClient = new HashMap<PartnerBadge, Client>();

	public List<PartnerBadge> getPartners() {
		synchronized (partners) {
			return partners;		
		}
	
	}

    public  void  addPartner(PartnerBadge badge) {
    	synchronized (partners) {
    		partners.add(badge);
//        String proxyHost = "localhost";//System.getProperty("http.proxyHost");
//        String proxyPort = "8888";//System.getProperty("http.proxyPort");
        	DefaultClientConfig config = new DefaultClientConfig();
       // config.getFeatures().put(ClientConfig.FEATURE_DISABLE_XML_SECURITY, true); //TODO : SWITCHED SECURE FEATURE ON FEATURE_DISABLE_XML_SECURITY 
        //based on "Feature 'http://javax.xml.XMLConstants/feature/secure-processing' is not recognized" error
//        config.getProperties().put(DefaultApacheHttpClientConfig.PROPERTY_PROXY_URI, "http://" + proxyHost + ":" + proxyPort);
//        Client client =new Client(new URLConnectionClientHandler(
//                new HttpURLConnectionFactory() {
//                    Proxy p = null;
//                    @Override
//                    public HttpURLConnection getHttpURLConnection(URL url)
//                            throws IOException {
//                        if (p == null) {
//                            if (System.getProperties().containsKey("http.proxyHost")) {
//                                p = new Proxy(Proxy.Type.HTTP,
//                                        new InetSocketAddress(
//                                        System.getProperty("http.proxyHost"),
//                                        Integer.getInteger("http.proxyPort", 80)));
//                            } else {
//                                p = Proxy.NO_PROXY;
//                            }
//                        }
//                        return (HttpURLConnection) url.openConnection(p);
//                    }
//                }), config); 
//        		
//        		
//        		
//        		
//        		
//        		
	        Client client= Client.create(config); // new Client(null, config);
	        partnerToClient.put(badge, client);
    	}
    }

	public synchronized void removePartner(PartnerBadge badge) {
		synchronized (partners) {
			partners.remove(badge);	
		}
	}
	
	public synchronized Client getClient(PartnerBadge badge) {
			return partnerToClient.get(badge);	
		
	}

	

}
