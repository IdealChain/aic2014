package com.github.aic2014.onion.directorynode;

import com.github.aic2014.onion.model.ChainNodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.*;

public class InMemoryChainNodeService implements DirectoryNodeService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final int CHAIN_LENGTH = 3;
    private List<ChainNodeInfo> chainNodeInfos = Collections.synchronizedList(new ArrayList<ChainNodeInfo>());

    @PostConstruct
    public void onStartup() {
        logger.info("Hmm... startup from service");
    }

    @Override
    public String registerChainNode(final ChainNodeInfo chainNodeInfo) {
        return chainNodeInfo != null ? chainNodeInfo.getId() : null;
    }

    @Override
    public void unregisterChainNode(String id) {
        assert id == null : "id must be non-null";
        this.chainNodeInfos.remove(findChainNodeInfo(id));
    }

    private ChainNodeInfo findChainNodeInfo(String id) {
        Optional<ChainNodeInfo> result = chainNodeInfos.stream().filter(cni -> cni.getId().equals(id)).findAny();
        return result.isPresent() ? result.get() : null;
    }

    @Override
    public ChainNodeInfo getChainNode(final String id) {
        assert id == null : "id must be non-null";
        return findChainNodeInfo(id);
    }

    @Override
    public Collection<ChainNodeInfo> getAllChainNodes() {
        List<ChainNodeInfo> copy = new ArrayList<ChainNodeInfo>(this.chainNodeInfos.size());
        copy.addAll(this.chainNodeInfos);
        return copy;
    }

    @Override
    public List<ChainNodeInfo> getChain() {
        List<ChainNodeInfo> chain = new ArrayList<ChainNodeInfo>(3);
        if (this.chainNodeInfos.size() < CHAIN_LENGTH) {
            throw new IllegalStateException("At least " + CHAIN_LENGTH +
                    " chain nodes must be registered to build a chain. " +
                    "Currently registered: " + this.chainNodeInfos.size());
        }

        //when using one RandomGenerator - it will not us numbers more than once
        Random randomGenerator = new Random();
        int first = randomGenerator.nextInt(this.chainNodeInfos.size());
        int second = randomGenerator.nextInt(this.chainNodeInfos.size());
        while (second == first)
            second = randomGenerator.nextInt(this.chainNodeInfos.size());
        int third = randomGenerator.nextInt(this.chainNodeInfos.size());
        while ((third == first) || (third == second))
            third = randomGenerator.nextInt(this.chainNodeInfos.size());

        chain.add(this.chainNodeInfos.get(first));
        chain.add(this.chainNodeInfos.get(second));
        chain.add(this.chainNodeInfos.get(third));

        return chain;
    }
}
