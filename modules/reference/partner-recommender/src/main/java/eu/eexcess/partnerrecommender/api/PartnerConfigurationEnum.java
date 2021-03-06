package eu.eexcess.partnerrecommender.api;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;

import eu.eexcess.config.PartnerConfiguration;
import eu.eexcess.dataformats.PartnerBadge;
import eu.eexcess.partnerdata.api.ITransformer;
import eu.eexcess.partnerdata.reference.Enrichment;
import eu.eexcess.partnerrecommender.api.keys.PartnerApiKeys;
import eu.eexcess.partnerrecommender.reference.PartnerRegistrationThread;

/**
 * Enum to cache objects that have not to be reloaded every startup
 * 
 * @author hziak
 *
 */
public enum PartnerConfigurationEnum {
	CONFIG;
	private final Logger logger;
	private boolean intializedFlag;
	private PartnerConfiguration partnerConfiguration;
	private PartnerConnectorApi partnerConnector;
	private ITransformer transformer;
	private Enrichment enricher;
	private Client clientJacksonJson;
	private Client clientJAXBContext;
	private Client clientDefault;
	private ObjectMapper objectMapper;
	private QueryGeneratorApi queryGenerator;
	private Thread regThread;

	private PartnerConfigurationEnum() {

		this.logger = Logger.getLogger(PartnerConfigurationEnum.class.getName());
		this.objectMapper = new ObjectMapper();
		ClientConfig configDefault = new DefaultClientConfig();
		ClientConfig configJacksonJson = new DefaultClientConfig();
		ClientConfig configJAXBContext = new DefaultClientConfig();
		configJacksonJson.getClasses().add(JacksonJsonProvider.class);
		configJAXBContext.getClasses().add(JAXBContext.class);
		clientJacksonJson = Client.create(configJacksonJson);
		clientJAXBContext = Client.create(configJAXBContext);
		clientDefault = Client.create(configDefault);

		ObjectMapper mapper = new ObjectMapper();
		try {
			/*
			 * Read global partner key file
			 */
			String eexcessPartnerKeyFile =System.getenv("EEXCESS_PARTNER_KEY_FILE");
			logger.log(Level.INFO,"Reading Api Keys from:" + eexcessPartnerKeyFile);
			if(eexcessPartnerKeyFile==null)
			logger.log(Level.INFO,"Environment variable \"EEXCESS_PARTNER_KEY_FILE\" has to be set");
			PartnerApiKeys partnerKeys = mapper.readValue(new File(eexcessPartnerKeyFile), PartnerApiKeys.class);
		
			/*
			 * Read partner configuration file
			 */
			
			URL resource = getClass().getResource("/partner-config.json");
			mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
			partnerConfiguration = mapper.readValue(
					new File(resource.getFile()), PartnerConfiguration.class);
			for (PartnerConfiguration badge : partnerKeys.getPartners()) {
					if(partnerConfiguration.systemId.equals(badge.systemId)){
						if(badge.partnerKey!=null){
							partnerConfiguration.partnerKey=badge.partnerKey;
						}
						if(badge.userName!=null){
							partnerConfiguration.userName=badge.userName;
						}if(badge.password!=null){
							partnerConfiguration.password=badge.password;
						}
					}
			}
			
			/*
			 * Configure the partner connector
			 */

			partnerConnector = (PartnerConnectorApi) Class.forName(
					partnerConfiguration.partnerConnectorClass).newInstance();

			/*
			 * Configure data transformer
			 */
			if (!partnerConfiguration.isTransformedNative) {
				transformer = (ITransformer) Class.forName(
						partnerConfiguration.transformerClass).newInstance();
				transformer.init(partnerConfiguration);

				/*
				 * Configure data enricher
				 */

				enricher = new Enrichment();
				enricher.init(partnerConfiguration);
			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Cannot initialize partner recommender", e);

		}
		try {
			this.queryGenerator = (QueryGeneratorApi) Class.forName(
					partnerConfiguration.queryGeneratorClass).newInstance();
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException e1) {
			logger.log(Level.SEVERE, "Cannot initialize partner recommender",
					e1);
		}
	}

	public void registerPartnerAtServer() {
		logger.log(
				Level.INFO,
				"Starting Partner Helper Thread:"
						+ PartnerConfigurationEnum.CONFIG
								.getPartnerConfiguration().systemId);
		this.regThread = new Thread(new PartnerRegistrationThread(
				partnerConfiguration));
		regThread.setName(PartnerConfigurationEnum.CONFIG.partnerConfiguration.systemId+" Registration Thread");
		regThread.start();
	}

	public void unregisterPartnerAtServer() {
		regThread.stop();
		PartnerBadge badge = PartnerConfigurationEnum.CONFIG.getBadge();
		DefaultClientConfig jClientconfig = new DefaultClientConfig();
		jClientconfig.getClasses().add(JacksonJsonProvider.class);
		Client client = new Client(new URLConnectionClientHandler(),
				jClientconfig);
		WebResource service = client
				.resource(partnerConfiguration.federatedRecommenderURI
						+ "unregister");
		logger.log(Level.INFO, "Unregistering Partner: " + badge.getSystemId()
				+ " at " + partnerConfiguration.federatedRecommenderURI);
		Builder builder = service.accept(MediaType.APPLICATION_JSON);
		builder.type(MediaType.APPLICATION_JSON)
				.post(PartnerBadge.class, badge);
	}

	public PartnerConfiguration getPartnerConfiguration() {
		return partnerConfiguration;
	}

	public PartnerConnectorApi getPartnerConnector() {
		return partnerConnector;
	}

	public ITransformer getTransformer() {
		return transformer;
	}

	public Enrichment getEnricher() {
		return enricher;
	}

	public void setEnricher(Enrichment enricher) {
		this.enricher = enricher;
	}

	// TODO: hziak + rrubien: check for optimization, client could be configured
	// here aswell
	public Client getClientJacksonJson() {
		return clientJacksonJson;
	}

	// TODO: hziak + rrubien: check for optimization, client could be configured
	// here aswell
	public Client getClientJAXBContext() {
		return clientJAXBContext;
	}

	// TODO: hziak + rrubien: check for optimization, client could be configured
	// here aswell
	public Client getClientDefault() {
		return clientDefault;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public QueryGeneratorApi getQueryGenerator() {
		return queryGenerator;
	}

	public PartnerBadge getBadge() {
		return partnerConfiguration;
	}

	public boolean getIntializedFlag() {
		return intializedFlag;
	}

	public void setIntializedFlag(boolean intializedFlag) {
		this.intializedFlag = intializedFlag;
	}

}
