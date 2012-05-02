/*
 * Copyright 2010-2011 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.billing.entitlement.api;

import com.google.common.base.Joiner;
import com.google.common.eventbus.Subscribe;
import com.ning.billing.entitlement.api.timeline.RepairEntitlementEvent;
import com.ning.billing.entitlement.api.user.SubscriptionEvent;
import com.ning.billing.util.bus.Bus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class ApiTestListener {

    private static final Logger log = LoggerFactory.getLogger(ApiTestListener.class);

    private final List<NextEvent> nextApiExpectedEvent;


    private boolean isApiCompleted;

    private boolean expectRepairCompletion;
    private boolean isRepairCompleted;    

    public enum NextEvent {
        MIGRATE_ENTITLEMENT,
        MIGRATE_BILLING,
        CREATE,
        RE_CREATE,
        CHANGE,
        CANCEL,
        PHASE
    }

    public ApiTestListener(Bus eventBus) {
        this.nextApiExpectedEvent = new Stack<NextEvent>();
        reset();
    }
    
    @Subscribe
    public void handleRepairEvent(RepairEntitlementEvent event) {
        log.debug("-> Got event RepairEntitlementEvent for bundle " + event.getBundleId());        
        if (!expectRepairCompletion) {
            log.error("Did not expect repair any event!!!");
        } else {
            synchronized(this) {
                isRepairCompleted = true;
                notify();
            }
        }
    }

    @Subscribe
    public void handleEntitlementEvent(SubscriptionEvent event) {
        switch (event.getTransitionType()) {
        case MIGRATE_ENTITLEMENT:
            subscriptionMigrated(event);
            break;
        case CREATE:
            subscriptionCreated(event);
            break;
        case RE_CREATE:
            subscriptionReCreated(event);
            break;
        case CANCEL:
            subscriptionCancelled(event);
            break;
        case CHANGE:
            subscriptionChanged(event);
            break;
        case UNCANCEL:
            break;
        case PHASE:
            subscriptionPhaseChanged(event);
            break;
        case MIGRATE_BILLING:
            subscriptionMigratedBilling(event);
            break;
        default:
            throw new RuntimeException("Unexpected event type " + event.getRequestedTransitionTime());
        }

    }

    public void pushNextApiExpectedEvent(NextEvent next) {
        synchronized (this) {
            nextApiExpectedEvent.add(next);
            isApiCompleted = false;
        }
    }
    
    public void expectRepairCompletion() {
        expectRepairCompletion = true;
        isRepairCompleted = false;
    }

    public boolean isRepairCompleted(long timeout) {
        synchronized (this) {
            if (isRepairCompleted) {
                return isRepairCompleted;
            }
            try {
                wait(timeout);
            } catch (Exception ignore) {
            }
        }
        if (!isRepairCompleted) {
            log.debug("ApiTestListener did not complete in " + timeout + " ms");
        }
        return isRepairCompleted;
    }
    
    public boolean isApiCompleted(long timeout) {
        synchronized (this) {
            if (isApiCompleted) {
                return isApiCompleted;
            }
            try {
                wait(timeout);
            } catch (Exception ignore) {
            }
        }
        if (!isApiCompleted) {
            log.debug("ApiTestListener did not complete in " + timeout + " ms");
        }
        return isApiCompleted;
    }

    public void reset() {
        nextApiExpectedEvent.clear();
        this.isApiCompleted = false;
        this.expectRepairCompletion = false;
        this.isRepairCompleted = false;
    }

    private void notifyIfStackEmpty() {
        log.debug("notifyIfStackEmpty ENTER");
        synchronized (this) {
            if (nextApiExpectedEvent.isEmpty()) {
                log.debug("notifyIfStackEmpty EMPTY");
                isApiCompleted = true;
                notify();
            }
        }
        log.debug("notifyIfStackEmpty EXIT");
    }

    private void assertEqualsNicely(NextEvent expected) {

        boolean foundIt = false;
        Iterator<NextEvent> it = nextApiExpectedEvent.iterator();
        while (it.hasNext()) {
            NextEvent ev = it.next();
            if (ev == expected) {
                it.remove();
                foundIt = true;
                break;
            }
        }

        if (!foundIt) {
            Joiner joiner = Joiner.on(" ");
            Assert.fail("Expected event " + expected + " got " + joiner.join(nextApiExpectedEvent));
        }
    }


    public void subscriptionMigrated(SubscriptionEvent migrated) {
        log.debug("-> Got event MIGRATED");
        assertEqualsNicely(NextEvent.MIGRATE_ENTITLEMENT);
        notifyIfStackEmpty();
    }

    public void subscriptionCreated(SubscriptionEvent created) {
        log.debug("-> Got event CREATED");
        assertEqualsNicely(NextEvent.CREATE);
        notifyIfStackEmpty();
    }

    public void subscriptionReCreated(SubscriptionEvent recreated) {
        log.debug("-> Got event RE_CREATED");
        assertEqualsNicely(NextEvent.RE_CREATE);
        notifyIfStackEmpty();
    }


    public void subscriptionCancelled(SubscriptionEvent cancelled) {
        log.debug("-> Got event CANCEL");
        assertEqualsNicely(NextEvent.CANCEL);
        notifyIfStackEmpty();
    }


    public void subscriptionChanged(SubscriptionEvent changed) {
        log.debug("-> Got event CHANGE");
        assertEqualsNicely(NextEvent.CHANGE);
        notifyIfStackEmpty();
    }


    public void subscriptionPhaseChanged(
            SubscriptionEvent phaseChanged) {
        log.debug("-> Got event PHASE");
        assertEqualsNicely(NextEvent.PHASE);
        notifyIfStackEmpty();
    }

    public void subscriptionMigratedBilling(SubscriptionEvent migrated) {
        log.debug("-> Got event MIGRATED_BLLING");
        assertEqualsNicely(NextEvent.MIGRATE_BILLING);
        notifyIfStackEmpty();
    }

}
