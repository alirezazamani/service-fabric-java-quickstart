package statefulservice;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import microsoft.servicefabric.services.communication.runtime.ServiceReplicaListener;

import microsoft.servicefabric.services.runtime.StatefulService;

import microsoft.servicefabric.services.remoting.Service;
import microsoft.servicefabric.services.remoting.fabrictransport.runtime.FabricTransportServiceRemotingListener;

import microsoft.servicefabric.data.ReliableStateManager;
import microsoft.servicefabric.data.Transaction;
import microsoft.servicefabric.data.collections.ReliableHashMap;
import microsoft.servicefabric.data.utilities.AsyncEnumeration;
import microsoft.servicefabric.data.utilities.KeyValuePair;

import system.fabric.StatefulServiceContext; 

import rpcmethods.VotingRPC; 

class VotingDataService extends StatefulService implements VotingRPC {
    private static final Logger logger = Logger.getLogger(VotingDataService.class.getName());
    private static final String MAP_NAME = "votesMap";
    private ReliableStateManager stateManager;
    
    protected VotingDataService (StatefulServiceContext statefulServiceContext) {
        super (statefulServiceContext);
    }

    @Override
    protected List<ServiceReplicaListener> createServiceReplicaListeners() {
        try {
            FileOutputStream fos = new FileOutputStream("/tmp/VotingDataService.txt", true);
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
            FileOutputStream fos = new FileOutputStream("/tmp/VotingDataService.txt", true);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            ReliableHashMap<String, String> votesMap = stateManager
                    .<String, String> getOrAddReliableHashMapAsync(MAP_NAME).get();
                        
            Transaction tx = stateManager.createTransaction();
            AsyncEnumeration<KeyValuePair<String, String>> kv = votesMap.keyValuesAsync(tx).get();
            while (kv.hasMoreElementsAsync().get()) {
                KeyValuePair<String, String> k = kv.nextElementAsync().get();
                tempMap.put(k.getKey(), k.getValue()); 
            }

            //for (Map.Entry<String, String> e: tempMap.entrySet()) {
                //bw.write("getList: " + e.getKey() + " : " + e.getValue() + "\n");
            //}
            bw.flush();
            bw.close();

            tx.close();                    
            

        } catch (Exception e) {
            e.printStackTrace();
        }  
        
        return CompletableFuture.completedFuture(tempMap);
    }
    
    public CompletableFuture<Integer> addItem(String itemToAdd) {
        AtomicInteger status = new AtomicInteger(-1); 

    	try {
            FileOutputStream fos = new FileOutputStream("/tmp/VotingDataService.txt", true);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write("AddItem: " + itemToAdd +"\n");
            
            ReliableHashMap<String, String> votesMap = stateManager
                    .<String, String> getOrAddReliableHashMapAsync(MAP_NAME).get();
            int total = 100;
            long startTime = System.currentTimeMillis();
            for (int i=0; i<total; i++) {
                String newItem = itemToAdd + i;
                Transaction tx = stateManager.createTransaction();
                votesMap.computeAsync(tx, newItem, (k, v) -> {
                    if (v == null) {
                        return "1";
                    } else {
                        int numVotes = Integer.parseInt(v);
                        numVotes = numVotes + 1;
                        return Integer.toString(numVotes);
                    }
                }).get();
                tx.commitAsync().get();
                tx.close();
            }

            long finishTime = System.currentTimeMillis();
            long totalTime = finishTime - startTime;
            long averageTIme = totalTime/total;
            bw.write("AddItemsTime Total: " + totalTime + " and average: " + averageTIme +"\n");
            bw.flush();
            bw.close();

            status.set(1);                            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return CompletableFuture.completedFuture(new Integer(status.get()));
    }
    
    public CompletableFuture<Integer> removeItem(String itemToRemove) {
        AtomicInteger status = new AtomicInteger(-1); 
        try {
            FileOutputStream fos = new FileOutputStream("/tmp/VotingDataService.txt", true);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write("RemoveItem: " + itemToRemove +"\n");
            bw.flush();
            bw.close();

            ReliableHashMap<String, String> votesMap = stateManager
                    .<String, String> getOrAddReliableHashMapAsync(MAP_NAME).get();
            
            Transaction tx = stateManager.createTransaction();
            votesMap.removeAsync(tx, itemToRemove).get();
            tx.commitAsync().get();
            tx.close();                    
            
            status.set(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return CompletableFuture.completedFuture(new Integer(status.get()));
    }
    
}