/*******************************************************************************
 * (C) Copyright 2016 Jérôme Comte and Dorian Cransac
 *  
 *  This file is part of djigger
 *  
 *  djigger is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  djigger is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with djigger.  If not, see <http://www.gnu.org/licenses/>.
 *
 *******************************************************************************/
package io.djigger.ui;

import io.djigger.client.AgentFacade;
import io.djigger.client.Facade;
import io.djigger.client.FacadeListener;
import io.djigger.client.JMXClientFacade;
import io.djigger.client.JstackLogTailFacade;
import io.djigger.client.ProcessAttachFacade;
import io.djigger.db.client.StoreClient;
import io.djigger.model.Capture;
import io.djigger.monitoring.java.instrumentation.InstrumentSubscription;
import io.djigger.monitoring.java.instrumentation.InstrumentationEvent;
import io.djigger.monitoring.java.model.ThreadInfo;
import io.djigger.store.Store;
import io.djigger.store.filter.StoreFilter;
import io.djigger.ui.SessionConfiguration.SessionParameter;
import io.djigger.ui.agentcontrol.SessionControlPane;
import io.djigger.ui.analyzer.AnalyzerGroupPane;
import io.djigger.ui.common.Closeable;
import io.djigger.ui.common.MonitoredExecution;
import io.djigger.ui.common.MonitoredExecutionRunnable;
import io.djigger.ui.common.NodePresentationHelper;
import io.djigger.ui.instrumentation.InstrumentationStatisticsCache;
import io.djigger.ui.model.SessionExport;
import io.djigger.ui.storebrowser.StoreBrowserPane;
import io.djigger.ui.threadselection.ThreadSelectionPane;

import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("serial")
public class Session extends JPanel implements FacadeListener, Closeable {
	
	private static final Logger logger = LoggerFactory.getLogger(Session.class);
	
	private final SessionConfiguration config;
		
    private Facade facade;
    
    private StoreClient storeClient;

	private final JSplitPane splitPane;

    private final AnalyzerGroupPane analyzerGroupPane;

    private final Store store;

    private final InstrumentationStatisticsCache statisticsCache;

    private final ArgumentParser options;
    
    private final StoreBrowserPane storeBrowserPane;

    private final ThreadSelectionPane threadSelectionPane;
    
	protected final NodePresentationHelper presentationHelper;
	
	private final MainFrame main;
	
	private boolean active;

    public Session(SessionConfiguration config, MainFrame main) {
		super(new BorderLayout());
		this.config = config;
		this.main = main;
		this.options = main.getOptions();
		
		store = new Store();
		statisticsCache = new InstrumentationStatisticsCache(store);
		presentationHelper = new NodePresentationHelper(statisticsCache);
        
        facade = createFacade(config);
        
        if(facade!=null) {
    		facade.addListener(this);
        }

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);        

        threadSelectionPane = new ThreadSelectionPane(this);
        splitPane.add(threadSelectionPane);
        
        analyzerGroupPane = new AnalyzerGroupPane(this, presentationHelper);
        splitPane.add(analyzerGroupPane);
        splitPane.setDividerLocation(300);
        add(splitPane);

        if(config.getType() == SessionType.STORE) {
        	storeBrowserPane = new StoreBrowserPane(this);
        	add(storeBrowserPane, BorderLayout.PAGE_START);
        } else {
        	storeBrowserPane = null;
        }
        
        add(new SessionControlPane(this),BorderLayout.PAGE_END);

        threadSelectionPane.initialize();
        analyzerGroupPane.initialize();
    }

	private Facade createFacade(SessionConfiguration config) {
		Facade facade;
		if(config.getType() == SessionType.AGENT) {
        	final Properties prop = new Properties();
        	if(config.getParameters().containsKey(SessionParameter.PROCESSID)) {        		
        		prop.put(ProcessAttachFacade.PROCESSID, config.getParameters().get(SessionParameter.PROCESSID));
        		facade = new ProcessAttachFacade(prop, false);
        	} else {        		
        		prop.put("host", config.getParameters().get(SessionParameter.HOSTNAME));
        		prop.put("port", config.getParameters().get(SessionParameter.PORT));
        		facade = new AgentFacade(prop, false);
        	}
        } else if (config.getType() == SessionType.FILE) {
        	Properties prop = new Properties();
        	prop.put(JstackLogTailFacade.FILE_PARAM, config.getParameters().get(SessionParameter.FILE));
        	prop.put(JstackLogTailFacade.START_AT_FILE_BEGIN_PARAM, "true");

        	facade = new JstackLogTailFacade(prop, false);
        } else if (config.getType() == SessionType.JMX) {
        	Properties prop = new Properties();
        	prop.put("host", config.getParameters().get(SessionParameter.HOSTNAME));
        	prop.put("port", config.getParameters().get(SessionParameter.PORT));
        	prop.put("username", config.getParameters().get(SessionParameter.USERNAME));
        	prop.put("password", config.getParameters().get(SessionParameter.PASSWORD));
			facade = new JMXClientFacade(prop, false);
		} else {
			facade = null;
		}
		return facade;
	}
    
    public void start() throws Exception {
    	if(facade!=null) {
    		MonitoredExecution execution = new MonitoredExecution(main.getFrame(), "Connecting... Please wait.", new MonitoredExecutionRunnable() {
    			@Override
    			public void run(MonitoredExecution execution) throws Exception {
    				facade.connect();
    			}
    		});
    		execution.run();
    		refreshAll();
    	} else {
    		if(getSessionType()==SessionType.STORE) {
    			storeClient = new StoreClient();
    			
    			Map<SessionParameter,String> params = config.getParameters();
    			String hostname = params.get(SessionParameter.HOSTNAME);
    			int port;
    			try{
    			port = Integer.parseInt(params.get(SessionParameter.PORT));
    			}catch(NumberFormatException e){
    				port = 27017;
    			}
    			storeClient.connect(hostname, port);
    		} else if (getSessionType()==SessionType.AGENT_CAPTURE) {
    			final File file = new File(config.getParameters().get(SessionParameter.FILE));
    			MonitoredExecution execution = new MonitoredExecution(main.getFrame(), "Opening session... Please wait.", new MonitoredExecutionRunnable() {
    				@Override
    				public void run(MonitoredExecution execution) {
    					SessionExport export = SessionExport.read(file);
    					store.addThreadInfos(export.getStore().queryThreadDumps(null));
    					store.addCaptures(export.getStore().queryCaptures(0, Long.MAX_VALUE));
    					store.addInstrumentationSamples(export.getStore().queryInstrumentationSamples(null));
    				}
    			});
    			execution.run();
    			refreshAll();
    		}    		
    	}
    	
    }
    
    public void configure() {}
    
    public Facade getFacade() {
		return facade;
	}

    public MainFrame getMain() {
		return main;
	}

	public SessionConfiguration getConfig() {
		return config;
	}

	public enum SessionType {    	
        FILE("Thread dumps files"),

        AGENT("Agent sessions"),
        
        JMX("JMX Connection"),

        AGENT_CAPTURE("Saved agent sessions"),
        
        STORE("Store");
        
        private final String description;

		private SessionType(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
    }

    public SessionType getSessionType() {
        return config.getType();
    }

    public Store getStore() {
        return store;
    }
    
    public InstrumentationStatisticsCache getStatisticsCache() {
    	return statisticsCache;
    }

    public void refreshAll() {
    	store.processBuffers();
    	statisticsCache.reload();
    	threadSelectionPane.refresh();
    	analyzerGroupPane.refresh();    	
    }
    
    public void showLineNumbers(boolean show) {
    	analyzerGroupPane.showLineNumbers(show);
    }
    
    public void showMinCallCounts(boolean show) {
    	presentationHelper.setShowMinCallCounts(show);
    	refreshAll();
    }

    public void onThreadSelection(StoreFilter filter) {
    	statisticsCache.setStoreFilter(filter);
    	statisticsCache.reload();
    	
    	analyzerGroupPane.setStoreFilter(filter);
    	analyzerGroupPane.refresh();
    }

    public void clear() {
    	store.clear();
    	refreshAll();
    }

	public ArgumentParser getOptions() {
		return options;
	}
	
	Capture currentCapture;
	public void setSampling(boolean state) {
		if(facade!=null) {
			facade.setSampling(state);
			if(state) {
				currentCapture = new Capture(facade.getSamplingInterval());
				store.addOrUpdateCapture(currentCapture);
			} else {
				currentCapture.setEnd(System.currentTimeMillis());
				store.addOrUpdateCapture(currentCapture);
			}
		}
	}
	
	public void setSamplingInterval(int interval) {
		if(facade!=null) {
			facade.setSamplingInterval(interval);
			if(facade.isSampling()) {
				currentCapture.setEnd(System.currentTimeMillis());
				store.addOrUpdateCapture(currentCapture);
				currentCapture = new Capture(facade.getSamplingInterval());
				store.addOrUpdateCapture(currentCapture);
			}
		}
	}
	
	public Set<InstrumentSubscription> getSubscriptions() {
		return facade!=null?facade.getInstrumentationSubscriptions():null;
	}

	public void addSubscription(InstrumentSubscription subscription) {
		if(facade!=null) {
			facade.addInstrumentation(subscription);
			fireSubscriptionChangeEvent();
		}	
	}

	public void removeSubscription(InstrumentSubscription subscription) {
		if(facade!=null) {
			facade.removeInstrumentation(subscription);
			fireSubscriptionChangeEvent();
		}
	}
	
	public List<SessionListener> listeners = new ArrayList<>();
	
	public void addListener(SessionListener listener) {
		listeners.add(listener);
	}
	
	public void fireSubscriptionChangeEvent() {
		for (SessionListener sessionListener : listeners) {
			sessionListener.subscriptionChange();
		}
	}

	public void startCapture(Capture capture) {
		store.addOrUpdateCapture(capture);
	}

	public void stopCapture(Capture capture) {
		store.addOrUpdateCapture(capture);
	}

	public AnalyzerGroupPane getAnalyzerGroupPane() {
		return analyzerGroupPane;
	}

    public String getSessionName() {
		return config.getName();
	}

	@Override
	public String toString() {
		return config.getName();
	}
	
	public void close() {
		if(facade!=null) {
			facade.destroy();			
		}
	}
	
	@Override
	public void threadInfosReceived(List<ThreadInfo> threads) {
		if(!main.isOutOfMemoryPreventionActive()) {
	        store.addThreadInfos(threads);
		} else {
			System.out.println("Ignoring incoming message to prevent JVM from OutOfMemory!");
		}
	}

	@Override
	public void instrumentationSamplesReceived(List<InstrumentationEvent> events) {
		if(!main.isOutOfMemoryPreventionActive()) {
	         store.addInstrumentationSamples(events);
		} else {
			System.out.println("Ignoring incoming message to prevent JVM from OutOfMemory!");
		}
	}

	@Override
	public void connectionClosed() {
		active = false;
		main.handleSessionEvent(this, SessionEvent.CONNECTION_CLOSED);
	}

	@Override
	public void connectionEstablished() {
		active = true;
		main.handleSessionEvent(this, SessionEvent.CONNECTION_ESTABLISHED);
	}

	public boolean isActive() {
		return active;
	}

	public StoreClient getStoreClient() {
		return storeClient;
	}
}
