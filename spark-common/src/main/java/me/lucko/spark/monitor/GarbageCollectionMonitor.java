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

package me.lucko.spark.monitor;

import com.sun.management.GarbageCollectionNotificationInfo;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

public class GarbageCollectionMonitor implements NotificationListener, AutoCloseable {

    private final TickMonitor tickMonitor;
    private final List<NotificationEmitter> emitters = new ArrayList<>();

    public GarbageCollectionMonitor(TickMonitor tickMonitor) {
        this.tickMonitor = tickMonitor;

        List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean bean : beans) {
            if (!(bean instanceof NotificationEmitter)) {
                continue;
            }

            NotificationEmitter notificationEmitter = (NotificationEmitter) bean;
            notificationEmitter.addNotificationListener(this, null, null);
            this.emitters.add(notificationEmitter);
        }
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        if (!notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
            return;
        }

        GarbageCollectionNotificationInfo data = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
        this.tickMonitor.onGc(data);
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
    }
}
