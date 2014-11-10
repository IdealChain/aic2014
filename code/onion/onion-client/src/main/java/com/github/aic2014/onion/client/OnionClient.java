package com.github.aic2014.onion.client;

import com.github.aic2014.onion.crypto.CryptoService;
import com.github.aic2014.onion.json.JsonUtils;
import com.github.aic2014.onion.model.ChainNodeInfo;
import com.github.aic2014.onion.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Client that accesses the directory node for obtaining a chain and then
 * sends an onion-routed request through the chain.
 */
public class OnionClient {
  private final Logger logger = LoggerFactory.getLogger(getClass());


  private URI directoryNodeUri;
  private URI quoteServerUri;
  private URI originatorUri;
  @Autowired
  private CryptoService cryptoService;
  @Autowired
  private PendingResponseService pendingResponseService;

  private RestTemplate restTemplate = new RestTemplate();
  private Map<UUID, Future<String>> pendingResponses = new HashMap<UUID, Future<String>>();

  public OnionClient() {
  }

  private ChainNodeInfo[] getChain() {
    String requestUri = directoryNodeUri.toString() + "/getChain";
    logger.info("requesting: {}", requestUri);
    ResponseEntity<ChainNodeInfo[]> newChainResult = restTemplate.getForEntity(requestUri, ChainNodeInfo[].class);
    ChainNodeInfo[] newChain = newChainResult.getBody();
    logger.debug("obtained new chain: {}", newChain);
    return newChain;
  }

  public Future<String> executeOnionRoutedHttpRequest(String request) {
    //get the chain for the request
    ChainNodeInfo[] chain = getChain();
    logger.info("obtained chain {}", Arrays.toString(chain));
    //we're bootstrapping the functionality. At first, we just send a normal http request
    //to the first chain node, which responds with a dummy response.

    //TODO: replace with http request
    String payload = "dummyContent";
    UUID chainId = UUID.randomUUID();
    SettableListenableFuture<String> response = new SettableListenableFuture<String>();
    PendingResponse pendingResponse = new PendingResponse(chainId, response, chain.length);
    pendingResponseService.addPendingResponse(chainId, pendingResponse);

    Message msg = buildMessage(chain, chainId, payload);
    logger.debug("sending this message: {}", msg);
    restTemplate.put(chain[0].getUri().toString() + "/request", msg);
    return response;
  }

  private Message buildMessage(final ChainNodeInfo[] chain, UUID chainId, final String payload) {
    logger.debug("building message for chain {} and payload {}", chain, payload);

    String lastPayload = payload;
    Message msg = null;
    //from last call to first call, create message object, serialize it and
    //add it encrypted as payload to the one before.
    //start by building the outermost
    for (int idx = chain.length - 1; idx >= 0; idx --) {
      msg = new Message();
      msg.setChainId(chainId);
      msg.setRecipient(chain[idx].getUri());
      if (idx > 0){
        msg.setSender(chain[idx-1].getUri());
      } else {
        //for the outermost message, the client is the sender
        msg.setSender(this.originatorUri);
      }
      msg.setHopsToGo(chain.length - 1 - idx);
      msg.setPayload(this.cryptoService.encrypt(lastPayload, null));
      logger.debug("message for chain step {}: {}", idx, msg);
      //now, convert the newly built message to a payload for the next message
      lastPayload = JsonUtils.toJSON(msg);
    }
    return msg;
  }


  public void setDirectoryNodeUri(final URI directoryNodeUri) {
    this.directoryNodeUri = directoryNodeUri;
  }

  public void setQuoteServerUri(URI quoteServerUri) {
    this.quoteServerUri = quoteServerUri;
  }

  public void setOriginatorUri(URI originatorUri) {
    this.originatorUri = originatorUri;
  }
}
