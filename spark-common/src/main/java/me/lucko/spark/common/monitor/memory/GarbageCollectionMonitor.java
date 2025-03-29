/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.common.monitor.memory;

import com.sun.management.GarbageCollectionNotificationInfo;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Monitoring process for garbage collections.
 */
public class GarbageCollectionMonitor implements NotificationListener, AutoCloseable {

    /** The registered listeners */
    private final List<Listener> listeners = new ArrayList<>();
    /** A list of the NotificationEmitters that feed information to this monitor. */
    private final List<NotificationEmitter> emitters = new ArrayList<>();

    public GarbageCollectionMonitor() {
        // Add ourselves as a notification listener for all GarbageCollectorMXBean that
        // support notifications.
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean instanceof NotificationEmitter) {
                NotificationEmitter notificationEmitter = (NotificationEmitter) bean;
                notificationEmitter.addNotificationListener(this, null, null);

                // Keep track of the notification emitters we subscribe to so
                // the listeners can be removed on #close
                this.emitters.add(notificationEmitter);
            }
        }
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        // we're only interested in GC notifications
        if (!notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
            return;
        }

        GarbageCollectionNotificationInfo data = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
        for (Listener listener : this.listeners) {
            listener.onGc(data);
        }
    }

    @Override
    public void close() {
        for (NotificationEmitter e : this.emitters) {
            try {
                e.removeNotificationListener(this);
            } catch (ListenerNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        this.emitters.clear();
        this.listeners.clear();
    }

    /**
     * A simple listener object for garbage collections.
     */
    public interface Listener {
        void onGc(GarbageCollectionNotificationInfo data);
    }

    /**
     * Gets a human-friendly description for the type of the given GC notification.
     *
     * @param info the notification object
     * @return the name of the GC type
     */
    public static String getGcType(GarbageCollectionNotificationInfo info) {
        if (info.getGcAction().equals("end of minor GC")) {
            return "Young Gen";
        } else if (info.getGcAction().equals("end of major GC")) {
            return "Old Gen";
        } else {
            return info.getGcAction();
        }
    }

}
