/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;

import java.util.SortedSet;

public class GCResultContext implements EventContext {

  private final int                 gcIteration;
  private final SortedSet<ObjectID> gcedOids;

  public GCResultContext(int gcIteration, SortedSet<ObjectID> gcedOids) {
    this.gcIteration = gcIteration;
    this.gcedOids = gcedOids;
  }

  public int getGCIterationCount() {
    return gcIteration;
  }

  public SortedSet<ObjectID> getGCedObjectIDs() {
    return gcedOids;
  }

  public String toString() {
    return "GCResultContext [ " + gcIteration + " , " + gcedOids.size() + " ]";
  }
}
