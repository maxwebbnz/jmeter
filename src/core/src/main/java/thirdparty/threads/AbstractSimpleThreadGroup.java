/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package thirdparty.threads;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.engine.TreeCloner;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterThread;
import org.apache.jmeter.threads.ListenerNotifier;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.ListedHashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSimpleThreadGroup extends AbstractThreadGroup implements Serializable{
    private static final Logger log = LoggerFactory.getLogger(AbstractSimpleThreadGroup.class);

    private static final long WAIT_TO_DIE = JMeterUtils.getPropDefault("jmeterengine.threadstop.wait", 5 * 1000); // 5 seconds
    public static final String THREAD_GROUP_DISTRIBUTED_PREFIX_PROPERTY_NAME = "__jm.D_TG"; // FIXME: use JMeterUtils.THREAD_GROUP_DISTRIBUTED_PREFIX_PROPERTY_NAME when dependency is updated
    protected final Map<JMeterThread, Thread> allThreads = new ConcurrentHashMap<>();
    /** Whether scheduler is being used */
    public static final String SCHEDULER = "ThreadGroup.scheduler";

    /** Scheduler duration, overrides end time */
    public static final String DURATION = "ThreadGroup.duration";
    public static final String DELAY = "ThreadGroup.delay";
    private transient Object addThreadLock = new Object();

    private int groupNumber;
    private volatile boolean running = false;

    private boolean isSameUserOnNextIteration = true;
    /** Thread safe class */
    private ListenerNotifier notifier;

    /** This property will be cloned */
    private ListedHashTree threadGroupTree;

    public AbstractSimpleThreadGroup(){
    }

    public boolean isSameUser(){
        return isSameUserOnNextIteration;
    }
    /*
     * MJW -  I don't think we need to redefine the scheduleThread logic here.
     *          the consumers of this class will redefine it anyway???
     *
     */

     @Override
     public void start(int groupNum, ListenerNotifier notifier, ListedHashTree threadGroupTree, StandardJMeterEngine engine){
        running = true;
        groupNumber = groupNum;
        this.notifier = notifier;
        this.threadGroupTree = threadGroupTree;

        int numThreads = getNumThreads();

        log.info("Starting thread group number " + groupNum + " threads " + numThreads);

        long now = System.currentTimeMillis(); // needs to be same time for all threads in the group
        final JMeterContext context = JMeterContextService.getContext();

        for (int i = 0; running && i < numThreads; i++) {
            // JMeterThread jmThread = makeThread(groupNum, notifier, threadGroupTree, engine, i, context);
            // ListenerNotifier notifier, ListedHashTree threadGroupTree, StandardJMeterEngine engine,
        // int threadNum, final JMeterContext context, long now, int delay, Boolean isSameUserOnNextIteration
            startNewThread(notifier, threadGroupTree, engine, i, context, now, 0, isSameUser());
            // scheduleThread(jmThread, now); // set start and end time
            // Thread newThread = new Thread(jmThread, jmThread.getThreadName());
            // registerStartedThread(jmThread, newThread);
            // newThread.start();
        }

        log.info("Started thread group number " + groupNum);
     }

    private void registerStartedThread(JMeterThread jMeterThread, Thread newThread) {
        allThreads.put(jMeterThread, newThread);
    }

    private ListedHashTree cloneTree(ListedHashTree tree) {
        TreeCloner cloner = new TreeCloner(true);
        tree.traverse(cloner);
        return cloner.getClonedTree();
    }

    private JMeterThread makeThread(
        ListenerNotifier notifier, ListedHashTree threadGroupTree,
        StandardJMeterEngine engine, int threadNumber,
        JMeterContext context, Boolean isSameUserOnNextIteration) { // N.B. Context needs to be fetched in the correct thread
        boolean onErrorStopTest = getOnErrorStopTest();
        boolean onErrorStopTestNow = getOnErrorStopTestNow();
        boolean onErrorStopThread = getOnErrorStopThread();
        boolean onErrorStartNextLoop = getOnErrorStartNextLoop();
        String groupName = getName();
        log.info(String.valueOf(isSameUser()));
        final JMeterThread jmeterThread = new JMeterThread(cloneTree(threadGroupTree), this, notifier, isSameUser());
        jmeterThread.setThreadNum(threadNumber);
        jmeterThread.setThreadGroup(this);
        jmeterThread.setInitialContext(context);
        String distributedPrefix =
                JMeterUtils.getPropDefault(JMeterUtils.THREAD_GROUP_DISTRIBUTED_PREFIX_PROPERTY_NAME, "");
        final String threadName = distributedPrefix + (distributedPrefix.isEmpty() ? "":"-") +groupName + " " + groupNumber + "-" + (threadNumber + 1);
        jmeterThread.setThreadName(threadName);
        jmeterThread.setEngine(engine);
        jmeterThread.setOnErrorStopTest(onErrorStopTest);
        jmeterThread.setOnErrorStopTestNow(onErrorStopTestNow);
        jmeterThread.setOnErrorStopThread(onErrorStopThread);
        jmeterThread.setOnErrorStartNextLoop(onErrorStartNextLoop);
        return jmeterThread;
    }

    /*
     *
     * MJW - JMeter 5.4.3 compared to 2.13 puts this logic to create threads seperately to the start class
     *       I think that's  a good idea...
     *
     */
    /**
     * Get whether scheduler is being used
     *
     * @return true if scheduler is being used
     */
    public boolean getScheduler() {
        return getPropertyAsBoolean(SCHEDULER);
    }

    /**
     * Get the desired duration of the thread group test run
     *
     * @return the duration (in secs)
     */
    public long getDuration() {
        return getPropertyAsLong(DURATION);
    }

    public long getDelay() {
        return getPropertyAsLong(DELAY);
    }

    private long tgStartTime = -1;
    private static final long TOLERANCE = 1000;

    protected abstract void scheduleThisThread(JMeterThread thread, long now);

    public void scheduleThisThread(JMeterThread thread) {
        if (System.currentTimeMillis() - tgStartTime > TOLERANCE) {
            tgStartTime = System.currentTimeMillis();
        }
        scheduleThisThread(thread, tgStartTime);
    }

     private JMeterThread startNewThread(ListenerNotifier notifier, ListedHashTree threadGroupTree, StandardJMeterEngine engine,
        int threadNum, final JMeterContext context, long now, int delay, Boolean isSameUserOnNextIteration) {
        JMeterThread jmThread = makeThread(notifier, threadGroupTree, engine, threadNum, context, isSameUserOnNextIteration);
        scheduleThisThread(jmThread, now); // set start and end time
        jmThread.setInitialDelay(delay);
        Thread newThread = new Thread(jmThread, jmThread.getThreadName());
        registerStartedThread(jmThread, newThread);
        newThread.start();
        return jmThread;
    }

    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public JMeterThread addNewThread(int delay, StandardJMeterEngine engine) {
        long now = System.currentTimeMillis();
        JMeterContext context = JMeterContextService.getContext();
        JMeterThread newJmThread;
        int numThreads;
        synchronized (addThreadLock) {
            numThreads = getNumThreads();
            setNumThreads(numThreads + 1);
        }
        newJmThread = startNewThread(this.notifier, this.threadGroupTree, engine, numThreads, context, now, delay, isSameUser());
        JMeterContextService.addTotalThreads( 1 );
        log.info("Started new thread in group {}", groupNumber);
        return newJmThread;
    }

    @Override
    public boolean stopThread(String threadName, boolean now) {
        for (Map.Entry<JMeterThread, Thread> threadEntry : allThreads.entrySet()) {
            JMeterThread jMeterThread = threadEntry.getKey();
            if (jMeterThread.getThreadName().equals(threadName)) {
                stopThread(jMeterThread, threadEntry.getValue(), now);
                return true;
            }
        }
        return false;
    }

        /**
     * Hard Stop JMeterThread thread and interrupt JVM Thread if interrupt is {@code true}
     * @param jmeterThread {@link JMeterThread}
     * @param jvmThread {@link Thread}
     * @param interrupt Interrupt thread or not
     */
    private void stopThread(JMeterThread jmeterThread, Thread jvmThread, boolean interrupt) {
        jmeterThread.stop();
        jmeterThread.interrupt(); // interrupt sampler if possible
        if (interrupt && jvmThread != null) { // Bug 49734
            jvmThread.interrupt(); // also interrupt JVM thread
        }
    }

    /**
     * Called by JMeterThread when it finishes
     */
    @Override
    public void threadFinished(JMeterThread thread) {
        if (log.isDebugEnabled()) {
            log.debug("Ending thread {}", thread.getThreadName());
        }
        allThreads.remove(thread);
    }

        /**
     * This is an immediate stop interrupting:
     * <ul>
     *  <li>current running threads</li>
     *  <li>current running samplers</li>
     * </ul>
     * For each thread, invoke:
     * <ul>
     * <li>{@link JMeterThread#stop()} - set stop flag</li>
     * <li>{@link JMeterThread#interrupt()} - interrupt sampler</li>
     * <li>{@link Thread#interrupt()} - interrupt JVM thread</li>
     * </ul>
     */
    @Override
    public void tellThreadsToStop() {
        tellThreadsToStop(true);
    }

    /**
     * This is a clean shutdown.
     * For each thread, invoke:
     * <ul>
     * <li>{@link JMeterThread#stop()} - set stop flag</li>
     * </ul>
     */
    @Override
    public void stop() {
        running = false;
        allThreads.keySet().forEach(JMeterThread::stop);
    }

    /**
     * @return number of active threads
     */
    @Override
    public int numberOfActiveThreads() {
        return allThreads.size();
    }

    /**
     * @return boolean true if all threads stopped
     */
    @Override
    public boolean verifyThreadsStopped() {
        boolean stoppedAll = true;
        /*
         * MJW - We don't use the capability to delayStartup of these custom thread groups
         *       in our tests
         */
        // if (delayedStartup) {
        //     stoppedAll = verifyThreadStopped(threadStarter);
        // }
        if(stoppedAll) {
            for (Thread t : allThreads.values()) {
                if(!verifyThreadStopped(t)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
    /**
     * Verify thread stopped and return true if stopped successfully
     * @param thread Thread
     * @return boolean
     */
    private boolean verifyThreadStopped(Thread thread) {
        boolean stopped = true;
        if (thread != null && thread.isAlive()) {
            try {
                thread.join(WAIT_TO_DIE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (thread.isAlive()) {
                stopped = false;
                if (log.isWarnEnabled()) {
                    log.warn("Thread won't exit: {}", thread.getName());
                }
            }
        }
        return stopped;
    }


    /**
     * Wait for all Group Threads to stop
     */
    @Override
    public void waitThreadsStopped() {
        /* @Bugzilla 60933
         * Threads can be added on the fly during a test into allThreads
         * we have to check if allThreads is really empty before stopping
         */
        while (!allThreads.isEmpty()) {
            allThreads.values().forEach(this::waitThreadStopped);
        }

    }

       /**
     * Wait for thread to stop
     * @param thread Thread
     */
    private void waitThreadStopped(Thread thread) {
        if (thread == null) {
            return;
        }
        while (thread.isAlive()) {
            try {
                thread.join(WAIT_TO_DIE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void tellThreadsToStop(boolean now) {
        running = false;

        allThreads.forEach((key, value) -> stopThread(key, value, now));
    }
}
