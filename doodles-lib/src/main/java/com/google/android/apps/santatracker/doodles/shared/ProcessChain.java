/*
 * Copyright 2019. Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.apps.santatracker.doodles.shared;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A chain of processes, which are executed in the order in which they are added. When a process is
 * finished, it is removed from the chain and the next process in line is executed.
 */
public class ProcessChain {
    private final ReentrantLock lock = new ReentrantLock();
    private Queue<Process> processes;

    public ProcessChain(Process p) {
        processes = new LinkedList<>();
        processes.add(p);
    }

    public static void updateChains(List<ProcessChain> processChains, float deltaMs) {
        // Remove finished chains.
        for (int i = processChains.size() - 1; i >= 0; i--) {
            ProcessChain chain = processChains.get(i);
            if (chain.isFinished()) {
                processChains.remove(i);
            }
        }
        // Update still-running chains.
        for (int i = 0; i < processChains.size(); i++) {
            processChains.get(i).update(deltaMs);
        }
    }

    public ProcessChain then(Process p) {
        lock.lock();
        try {
            processes.add(p);
        } finally {
            lock.unlock();
        }
        return this;
    }

    public ProcessChain then(ProcessChain pc) {
        lock.lock();
        try {
            processes.addAll(pc.processes);
        } finally {
            lock.unlock();
        }
        return this;
    }

    public void update(float deltaMs) {
        lock.lock();
        try {
            final Process activeProcess = processes.peek();
            if (activeProcess != null) {
                activeProcess.update(deltaMs);

                if (activeProcess.isFinished()) {
                    processes.remove();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isFinished() {
        lock.lock();
        try {
            return processes.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}
