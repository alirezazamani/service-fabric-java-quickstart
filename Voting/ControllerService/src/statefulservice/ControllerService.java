package statefulservice;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import microsoft.servicefabric.services.communication.runtime.ServiceReplicaListener;

import microsoft.servicefabric.services.remoting.client.ServiceProxyBase;
import microsoft.servicefabric.services.runtime.StatefulService;

import microsoft.servicefabric.services.remoting.Service;
import microsoft.servicefabric.services.client.ServicePartitionKey;
import microsoft.servicefabric.services.communication.client.TargetReplicaSelector;
import microsoft.servicefabric.services.remoting.fabrictransport.runtime.FabricTransportServiceRemotingListener;

import microsoft.servicefabric.data.ReliableStateManager;
import microsoft.servicefabric.data.Transaction;
import microsoft.servicefabric.data.collections.ReliableHashMap;
import microsoft.servicefabric.data.utilities.AsyncEnumeration;
import microsoft.servicefabric.data.utilities.KeyValuePair;


import system.fabric.StatefulServiceContext; 

import rpcmethods.VotingRPC; 

class ControllerService extends StatefulService implements VotingRPC {
    private static final Logger logger = Logger.getLogger(ControllerService.class.getName());
    private static final String MAP_NAME = "votesMap";
    private ReliableStateManager stateManager;
    private static final List<String> partitionList = new ArrayList<>(Arrays.asList("Partition0", "Partition1", "Partition2"));
    
    protected ControllerService (StatefulServiceContext statefulServiceContext) {
        super (statefulServiceContext);
    }

    @Override
    protected List<ServiceReplicaListener> createServiceReplicaListeners() {
        try {
            FileOutputStream fos = new FileOutputStream("/tmp/ControllerService.txt", true);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write("createServiceReplicaListeners:" + getServiceContext().getServiceTypeName() + ":" + getServiceContext().getPartitionId() + ":" + getServiceContext().getReplicaId() + ":" + "\n");
            bw.flush();
        } catch (Exception e) {
            // pass
        }
        this.stateManager = this.getReliableStateManager();
        ArrayList<ServiceReplicaListener> listeners = new ArrayList<>();
        listeners.add(new ServiceReplicaListener((context) -> {
            return new FabricTransportServiceRemotingListener(context,this);
        }));
        return listeners;
    }
    
    public CompletableFuture<HashMap<String,String>> getList() {
        HashMap<String, String> tempMap = new HashMap<String, String>();

        try {
            FileOutputStream fos = new FileOutputStream("/tmp/ControllerService.txt", true);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            ReliableHashMap<String, String> votesMap = stateManager
                    .<String, String> getOrAddReliableHashMapAsync(MAP_NAME).get();

            ServicePartitionKey partitionKey = new ServicePartitionKey("Partition0");
            HashMap<String,String> list = ServiceProxyBase.create(VotingRPC.class, new URI("fabric:/VotingApplication/VotingDataService"), partitionKey, TargetReplicaSelector.DEFAULT, "").getList().get();
            //partitionKey = new ServicePartitionKey(partitionList.get(1));
            //list.putAll(ServiceProxyBase.create(VotingRPC.class, new URI("fabric:/VotingApplication/VotingDataService"), partitionKey, TargetReplicaSelector.DEFAULT, "").getList().get());
            //partitionKey = new ServicePartitionKey(partitionList.get(2));
            //list.putAll(ServiceProxyBase.create(VotingRPC.class, new URI("fabric:/VotingApplication/VotingDataService"), partitionKey, TargetReplicaSelector.DEFAULT, "").getList().get());

            for (Map.Entry<String,String> e: list.entrySet()) {
                tempMap.put(e.getKey(), e.getValue());
            }

            for (Map.Entry<String, String> e: tempMap.entrySet()) {
                bw.write("getList: " + e.getKey() + " : " + e.getValue() + "\n");
            }
            bw.flush();
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }  
        
        return CompletableFuture.completedFuture(tempMap);
    }
    
    public CompletableFuture<Integer> addItem(String itemToAdd) {
        AtomicInteger status = new AtomicInteger(-1); 

    	try {
            FileOutputStream fos = new FileOutputStream("/tmp/ControllerService.txt", true);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write("AddItem: " + itemToAdd +"\n");


            int index = itemToAdd.length()%3;
            int total = 1;

            ServicePartitionKey partitionKey = new ServicePartitionKey("Partition0");
            long startTime = System.currentTimeMillis();
            Integer num = ServiceProxyBase.create(VotingRPC.class, new URI("fabric:/VotingApplication/VotingDataService"), partitionKey, TargetReplicaSelector.DEFAULT, "").addItem(itemToAdd).get();
            long finishTime = System.currentTimeMillis();
            long totalTime = finishTime - startTime;
            bw.write("AddItemsTime Total: " + totalTime + " and average: " + totalTime/total +"\n");
            bw.flush();
            bw.close();
            status.set(1);                            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return CompletableFuture.completedFuture(status.get());
    }
    
    public CompletableFuture<Integer> removeItem(String itemToRemove) {
        AtomicInteger status = new AtomicInteger(-1); 
        try {
            FileOutputStream fos = new FileOutputStream("/tmp/ControllerService.txt", true);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write("RemoveItem: " + itemToRemove +"\n");
            int index = itemToRemove.length()%3;
            ServicePartitionKey partitionKey = new ServicePartitionKey("Partition0");
            bw.write("RemoveItem" + " : " + partitionKey.value() + "\n");
            bw.flush();
            bw.close();
            Integer num = ServiceProxyBase.create(VotingRPC.class, new URI("fabric:/VotingApplication/VotingDataService"), partitionKey, TargetReplicaSelector.DEFAULT, "").removeItem(itemToRemove).get();
            
            status.set(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return CompletableFuture.completedFuture(status.get());
    }
    
}