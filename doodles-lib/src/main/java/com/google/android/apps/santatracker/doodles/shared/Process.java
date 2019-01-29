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

/**
 * A generic class for running some piece of code. This class is most useful when used inside of a
 * process chain, which allows you explicitly define portions of code which should be run in serial
 * (generally inside of the update loop).
 */
public abstract class Process {

    public Process() {}

    /**
     * The outer update function for this process. Note that, when implementing the logic for the
     * process, updateLogic() should generally be overridden instead of update().
     *
     * @param deltaMs
     */
    public void update(float deltaMs) {
        if (!isFinished()) {
            updateLogic(deltaMs);
        }
    }

    public ProcessChain then(Process other) {
        return new ProcessChain(this).then(other);
    }

    public ProcessChain then(ProcessChain pc) {
        return new ProcessChain(this).then(pc);
    }

    public abstract void updateLogic(float deltaMs);

    public abstract boolean isFinished();
}
