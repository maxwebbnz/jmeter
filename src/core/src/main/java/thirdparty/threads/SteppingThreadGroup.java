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

import org.apache.jmeter.threads.JMeterThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SteppingThreadGroup
        extends AbstractSimpleThreadGroup {

    private static final Logger log = LoggerFactory.getLogger(SteppingThreadGroup.class);
    private static final String THREAD_GROUP_DELAY = "Threads initial delay";
    private static final String INC_USER_PERIOD = "Start users period";
    private static final String INC_USER_COUNT = "Start users count";
    private static final String INC_USER_COUNT_BURST = "Start users count burst";
    private static final String DEC_USER_PERIOD = "Stop users period";
    private static final String DEC_USER_COUNT = "Stop users count";
    private static final String FLIGHT_TIME = "flighttime";
    private static final String RAMPUP = "rampUp";
    private int transcationTarget = 0;


    protected void scheduleThisThread(JMeterThread thread, long tgStartTime) {

        int inUserCount = getInUserCountAsInt();
        int outUserCount = getOutUserCountAsInt();

        if (inUserCount == 0) {
            inUserCount = getNumThreads();
        }
        if (outUserCount == 0) {
            outUserCount = getNumThreads();
        }

        int inUserCountBurst = Math.min(getInUserCountBurstAsInt(), getNumThreads());
        if (inUserCountBurst <= 0) {
            inUserCountBurst = inUserCount;
        }

        int rampUpBucket = thread.getThreadNum() < inUserCountBurst ? 0
                : 1 + (thread.getThreadNum() - inUserCountBurst) / inUserCount;
        int rampUpBucketThreadCount = thread.getThreadNum() < inUserCountBurst ? inUserCountBurst : inUserCount;

        long threadGroupDelay = 1000L * getThreadGroupDelayAsInt();
        long ascentPoint = tgStartTime + threadGroupDelay;
        long inUserPeriod = 1000L * getInUserPeriodAsInt();
        long additionalRampUp = 1000L * getRampUpAsInt() / rampUpBucketThreadCount;
        long flightTime = 1000L * getFlightTimeAsInt();
        long outUserPeriod = 1000L * getOutUserPeriodAsInt();

        long rampUpDuration = 1000L * getRampUpAsInt();
        long iterationDuration = inUserPeriod + rampUpDuration;
        //number of complete iteration, ie full (in user time + rampup duration) used
        int iterationCountTotal = getNumThreads() < inUserCountBurst ? 1
                : (int) Math.ceil((double) (getNumThreads() - inUserCountBurst) / inUserCount);

        int lastIterationUserCount = (getNumThreads() - inUserCountBurst) % inUserCount;
        if (lastIterationUserCount == 0) {
            lastIterationUserCount = inUserCount;
        }
        long descentPoint = ascentPoint + iterationCountTotal * iterationDuration + (1000L * getRampUpAsInt() / inUserCount) * lastIterationUserCount + flightTime;

        long rampUpBucketStartTime = ascentPoint + rampUpBucket * iterationDuration;
        int rampUpBucketThreadPosition = thread.getThreadNum() < inUserCountBurst ? thread.getThreadNum()
                : (thread.getThreadNum() - inUserCountBurst) % inUserCount;
        long startTime = rampUpBucketStartTime + rampUpBucketThreadPosition * additionalRampUp;
        long endTime = descentPoint + outUserPeriod * (int) Math.floor((double) thread.getThreadNum() / outUserCount);

        log.debug(String.format("threadNum=%d, rampUpBucket=%d, rampUpBucketThreadCount=%d, rampUpBucketStartTime=%d, rampUpBucketThreadPosition=%d, rampUpDuration=%d, iterationDuration=%d, iterationCountTotal=%d, ascentPoint=%d, descentPoint=%d, startTime=%d, endTime=%d",
                thread.getThreadNum(), rampUpBucket, rampUpBucketThreadCount, rampUpBucketStartTime, rampUpBucketThreadPosition, rampUpDuration, iterationDuration, iterationCountTotal, ascentPoint, descentPoint, startTime, endTime));

        thread.setStartTime(startTime);
        thread.setEndTime(endTime);
        thread.setScheduled(true);
    }

    public String getThreadGroupDelay() {
        return getPropertyAsString(THREAD_GROUP_DELAY);
    }

    public void setThreadGroupDelay(String delay) {
        setProperty(THREAD_GROUP_DELAY, delay);
    }

    public String getInUserPeriod() {
        return getPropertyAsString(INC_USER_PERIOD);
    }

    public void setInUserPeriod(String value) {
        setProperty(INC_USER_PERIOD, value);
    }

    public String getInUserCount() {
        return getPropertyAsString(INC_USER_COUNT);
    }

    public void setInUserCount(String delay) {
        setProperty(INC_USER_COUNT, delay);
    }

    public String getInUserCountBurst() {
        return getPropertyAsString(INC_USER_COUNT_BURST);
    }

    public void setInUserCountBurst(String text) {
        setProperty(INC_USER_COUNT_BURST, text);
    }

    public String getFlightTime() {
        return getPropertyAsString(FLIGHT_TIME);
    }

    public void setFlightTime(String delay) {
        setProperty(FLIGHT_TIME, delay);
    }

    public String getOutUserPeriod() {
        return getPropertyAsString(DEC_USER_PERIOD);
    }

    public void setOutUserPeriod(String delay) {
        setProperty(DEC_USER_PERIOD, delay);
    }

    public String getOutUserCount() {
        return getPropertyAsString(DEC_USER_COUNT);
    }

    public void setOutUserCount(String delay) {
        setProperty(DEC_USER_COUNT, delay);
    }

    public String getRampUp() {
        return getPropertyAsString(RAMPUP);
    }

    public void setRampUp(String delay) {
        setProperty(RAMPUP, delay);
    }

    public int getThreadGroupDelayAsInt() {
        return getPropertyAsInt(THREAD_GROUP_DELAY);
    }

    public int getInUserPeriodAsInt() {
        return getPropertyAsInt(INC_USER_PERIOD);
    }

    public int getInUserCountAsInt() {
        return getPropertyAsInt(INC_USER_COUNT);
    }

    public int getInUserCountBurstAsInt() {
        return getPropertyAsInt(INC_USER_COUNT_BURST);
    }

    public int getRampUpAsInt() {
        return getPropertyAsInt(RAMPUP);
    }

    public void setTransactionTarget(String value){
        // For now we can just assume this is an hour count;
       transcationTarget = Integer.valueOf(value);
    }
    public int getFlightTimeAsInt() {
        return getPropertyAsInt(FLIGHT_TIME);
    }

    public int getOutUserPeriodAsInt() {
        return getPropertyAsInt(DEC_USER_PERIOD);
    }

    public int getOutUserCountAsInt() {
        return getPropertyAsInt(DEC_USER_COUNT);
    }

    public void setNumThreads(String execute) {
        setProperty(NUM_THREADS, execute);
    }

    public String getNumThreadsAsString() {
        return getPropertyAsString(NUM_THREADS);
    }
}
