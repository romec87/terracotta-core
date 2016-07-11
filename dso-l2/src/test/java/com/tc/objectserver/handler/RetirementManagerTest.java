/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.entity.EntityMessage;

import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;
import org.junit.Assert;

import java.util.List;


public class RetirementManagerTest {
  private RetirementManager retirementManager;


  @Before
  public void setUp() throws Exception {
    this.retirementManager = new RetirementManager();
  }

  @Test
  public void testSimpleRetire() throws Exception {
    ServerEntityRequest request = makeRequest();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    int concurrencyKey = 1;
    this.retirementManager.registerWithMessage(request, invokeMessage, concurrencyKey);
    
    List<ServerEntityRequest> toRetire = this.retirementManager.retireForCompletion(invokeMessage);
    Assert.assertEquals(1, toRetire.size());
    Assert.assertEquals(request, toRetire.get(0));
  }

  @Test
  public void testSequenceOfRetires() throws Exception {
    int concurrencyKey = 1;
    for (int i = 0; i < 10; ++i) {
      sendNormalMessage(concurrencyKey);
    }
  }

  private void sendNormalMessage(int concurrencyKey) {
    ServerEntityRequest request = makeRequest();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    this.retirementManager.registerWithMessage(request, invokeMessage, concurrencyKey);
    
    List<ServerEntityRequest> toRetire = this.retirementManager.retireForCompletion(invokeMessage);
    Assert.assertEquals(1, toRetire.size());
    Assert.assertEquals(request, toRetire.get(0));
  }

  @Test
  public void testDeferredRetire() throws Exception {
    ServerEntityRequest request = makeRequest();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    int concurrencyKey = 1;
    this.retirementManager.registerWithMessage(request, invokeMessage, concurrencyKey);
    
    ServerEntityRequest newRequest = makeRequest();
    EntityMessage newMessage = mock(EntityMessage.class);
    this.retirementManager.deferRetirement(invokeMessage, newMessage);
    
    List<ServerEntityRequest> toRetire = this.retirementManager.retireForCompletion(invokeMessage);
    Assert.assertEquals(0, toRetire.size());
    
    this.retirementManager.registerWithMessage(newRequest, newMessage, concurrencyKey);
    toRetire = this.retirementManager.retireForCompletion(newMessage);
    Assert.assertEquals(2, toRetire.size());
  }

  @Test
  public void testSequenceAndDefer() throws Exception {
    int concurrencyKey = 1;
    sendNormalMessage(concurrencyKey);
    
    ServerEntityRequest request = makeRequest();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    this.retirementManager.registerWithMessage(request, invokeMessage, concurrencyKey);
    
    ServerEntityRequest newRequest = makeRequest();
    EntityMessage newMessage = mock(EntityMessage.class);
    this.retirementManager.deferRetirement(invokeMessage, newMessage);
    
    List<ServerEntityRequest> toRetire = this.retirementManager.retireForCompletion(invokeMessage);
    Assert.assertEquals(0, toRetire.size());
    
    this.retirementManager.registerWithMessage(newRequest, newMessage, concurrencyKey);
    toRetire = this.retirementManager.retireForCompletion(newMessage);
    Assert.assertEquals(2, toRetire.size());
    
    sendNormalMessage(concurrencyKey);
    sendNormalMessage(concurrencyKey);
  }

  @Test
  public void testDeferredWithNonDeferred() throws Exception {
    ServerEntityRequest request = makeRequest();
    EntityMessage invokeMessage = mock(EntityMessage.class);
    int concurrencyKey = 1;
    this.retirementManager.registerWithMessage(request, invokeMessage, concurrencyKey);
    
    ServerEntityRequest deferRequest = makeRequest();
    EntityMessage deferMessage = mock(EntityMessage.class);
    this.retirementManager.deferRetirement(invokeMessage, deferMessage);
    
    List<ServerEntityRequest> toRetire = this.retirementManager.retireForCompletion(invokeMessage);
    Assert.assertEquals(0, toRetire.size());
    
    // Run some other messages.
    ServerEntityRequest request1 = makeRequest();
    EntityMessage message1 = mock(EntityMessage.class);
    this.retirementManager.registerWithMessage(request1, message1, concurrencyKey);
    toRetire = this.retirementManager.retireForCompletion(message1);
    Assert.assertEquals(0, toRetire.size());
    ServerEntityRequest request2 = makeRequest();
    EntityMessage message2 = mock(EntityMessage.class);
    this.retirementManager.registerWithMessage(request2, message2, concurrencyKey);
    toRetire = this.retirementManager.retireForCompletion(message2);
    Assert.assertEquals(0, toRetire.size());
    
    // Now, run the message which should unblock us.
    this.retirementManager.registerWithMessage(deferRequest, deferMessage, concurrencyKey);
    toRetire = this.retirementManager.retireForCompletion(deferMessage);
    Assert.assertEquals(4, toRetire.size());
    // check retiring requests order as well
    Assert.assertThat(toRetire, IsIterableContainingInOrder.contains(request, request1, request2, deferRequest));
  }

  /**
   *
   * <p>This test tests RetirementManager using EntityMessages with cross concurrency key dependencies</p>
   *
   * <p>Below diagram shows relationship between requests and concurrency keys</p>
   *
   * <pre>
   *                          ---------------------------------
   * Concurrency Key 1  -->  | request1 | request2 | request7
   *                          ---------------------------------
   * Concurrency Key 2  -->  | request3 | request4
   *                          ---------------------------------
   * Concurrency Key 3  -->  | request5 | request6
   *                          ---------------------------------
   * </pre>
   *
   * <p>Deferred requests</p>
   *  <ul>
   *    <li>request1 deferred its retirement to request3</li>
   *    <li>request3 deferred its retirement to request5</li>
   *  </ul>
   *
   *
   * @throws Exception
   */
  @Test
  public void testDeferredMessageWithCrossConcurrencyKeyDependencies() throws Exception {

    int concurrencyKeyOne = 1;
    int concurrencyKeyTwo = 2;
    int concurrencyKeyThree = 3;
    List<ServerEntityRequest> toRetire;

    ServerEntityRequest request1 = makeRequest();
    EntityMessage request1Message = mock(EntityMessage.class);
    this.retirementManager.registerWithMessage(request1, request1Message, concurrencyKeyOne);

    ServerEntityRequest request2 = makeRequest();
    EntityMessage request2Message = mock(EntityMessage.class);
    this.retirementManager.registerWithMessage(request2, request2Message, concurrencyKeyOne);
    toRetire = this.retirementManager.retireForCompletion(request2Message);
    // Completing request2 shouldn't cause any messages to be retired as request1 is not retired yet
    Assert.assertEquals(0, toRetire.size());

    ServerEntityRequest request3 = makeRequest();
    EntityMessage request3Message = mock(EntityMessage.class);
    // request1 retirement deferred until request3 completes now
    this.retirementManager.deferRetirement(request1Message, request3Message);
    this.retirementManager.registerWithMessage(request3, request3Message, concurrencyKeyTwo);

    toRetire = this.retirementManager.retireForCompletion(request1Message);
    // Completing request1 shouldn't cause any messages to be retired as request3 is not completed yet
    Assert.assertEquals(0, toRetire.size());

    ServerEntityRequest request4 = makeRequest();
    EntityMessage request4Message = mock(EntityMessage.class);
    this.retirementManager.registerWithMessage(request4, request4Message, concurrencyKeyTwo);
    toRetire = this.retirementManager.retireForCompletion(request4Message);
    // Completing request4 shouldn't cause any messages to be retired as request3 is not retired yet
    Assert.assertEquals(0, toRetire.size());

    ServerEntityRequest request5 = makeRequest();
    EntityMessage request5Message = mock(EntityMessage.class);
    // request3 retirement deferred until request5 completes now
    this.retirementManager.deferRetirement(request3Message, request5Message);
    this.retirementManager.registerWithMessage(request5, request5Message, concurrencyKeyThree);

    toRetire = this.retirementManager.retireForCompletion(request3Message);
    // Completing request3 should retire both request1 and request2 in order but not request3
    Assert.assertEquals(2, toRetire.size());
    Assert.assertThat(toRetire, IsIterableContainingInOrder.contains(request1, request2));

    ServerEntityRequest request6 = makeRequest();
    EntityMessage request6Message = mock(EntityMessage.class);
    this.retirementManager.registerWithMessage(request6, request6Message, concurrencyKeyThree);
    toRetire = this.retirementManager.retireForCompletion(request6Message);
    // Completing request6 shouldn't cause any messages to be retired as request5 is not retired yet
    Assert.assertEquals(0, toRetire.size());

    ServerEntityRequest request7 = makeRequest();
    EntityMessage request7Message = mock(EntityMessage.class);
    this.retirementManager.deferRetirement(request5Message, request7Message);
    toRetire = this.retirementManager.retireForCompletion(request5Message);
    // Completing request5 should retire both request3 and request4 in order
    Assert.assertEquals(2, toRetire.size());
    Assert.assertThat(toRetire, IsIterableContainingInOrder.contains(request3, request4));

    this.retirementManager.registerWithMessage(request7, request7Message, concurrencyKeyOne);
    toRetire = this.retirementManager.retireForCompletion(request7Message);
    // Completing request7 should retire request7, request5 and request6 in order
    // Note that we need to maintain order only for request5 and request6 as they belong to same
    // concurrency key
    Assert.assertEquals(3, toRetire.size());
    Assert.assertThat(toRetire, IsIterableContainingInOrder.contains(request7, request5, request6));

  }

  @Test
  public void testRetireForCompletionWithUncompletedRequests() throws Exception {
    int concurrencyKeyOne = 1;
    int concurrencyKeyTwo = 2;
    List<ServerEntityRequest> toRetire;

    ServerEntityRequest request1 = makeRequest();
    EntityMessage request1Message = mock(EntityMessage.class);
    this.retirementManager.registerWithMessage(request1, request1Message, concurrencyKeyOne);

    ServerEntityRequest request2 = makeRequest();
    EntityMessage request2Message = mock(EntityMessage.class);
    this.retirementManager.registerWithMessage(request2, request2Message, concurrencyKeyOne);

    ServerEntityRequest request3 = makeRequest();
    EntityMessage request3Message = mock(EntityMessage.class);
    // request1 retirement deferred until request3 completes
    this.retirementManager.deferRetirement(request1Message, request3Message);
    this.retirementManager.registerWithMessage(request3, request3Message, concurrencyKeyTwo);

    toRetire = this.retirementManager.retireForCompletion(request1Message);
    // request1 completion shouldn't retire any requests as request3 is not completed yet
    Assert.assertEquals(0, toRetire.size());

    toRetire = this.retirementManager.retireForCompletion(request3Message);
    // request3 completion should only retire request1 and request3 as request2 is not completed yet
    Assert.assertEquals(2, toRetire.size());
    Assert.assertThat(toRetire, IsIterableContainingInOrder.contains(request3, request1));

    toRetire = this.retirementManager.retireForCompletion(request2Message);
    Assert.assertEquals(1, toRetire.size());
    // request2 completion will retire request2 finally
    Assert.assertThat(toRetire, IsIterableContainingInOrder.contains(request2));


  }

  private ServerEntityRequest makeRequest() {
    ServerEntityRequest request = mock(ServerEntityRequest.class);
    when(request.getAction()).thenReturn(ServerEntityAction.INVOKE_ACTION);
    return request;
  }
}
