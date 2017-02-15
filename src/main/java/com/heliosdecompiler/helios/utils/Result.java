/*
 * Copyright 2017 Sam Sun <github-contact@samczsun.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.heliosdecompiler.helios.utils;

public interface Result {
    /**
     * Success!
     */
    OperationResultCreator SUCCESS = new DefaultOperationResultCreator(Type.SUCCESS);
    /**
     * The setting could not be validated due to some error
     * <p>
     * info[0] = Throwable
     * <p>
     * Used in {@link SettingsValidator}
     */
    OperationResultCreator ERROR_OCCURED = new InformationalOperationResultCreator(Type.ERROR_OCCURED);
    /**
     * Python 2 was not set
     */
    OperationResultCreator NO_PYTHON2_SET = new DefaultOperationResultCreator(Type.NO_PYTHON2_SET);
    /**
     * Could not run the process
     * <p>
     * info[0] = Throwable
     * info[1] = String (process log)
     */
    OperationResultCreator ERROR_OCCURED_IN_PROCESS = new InformationalOperationResultCreator(Type.ERROR_OCCURED_IN_PROCESS);
    /**
     * Could not decompile
     * <p>
     * info[0] = String
     */
    OperationResultCreator ERROR_OCCURED_WHILE_DECOMPILING = new InformationalOperationResultCreator(Type.ERROR_OCCURED_WHILE_DECOMPILING);

    Type getType();

    default boolean is(Type type) {
        return getType() == type;
    }

    default boolean not(Type type) {
        return getType() != type;
    }

    enum Type {
        SUCCESS,
        NO_SETTING_SPECIFIED,
        ERROR_OCCURED,
        INVALID_SETTING,
        NO_PYTHON2_SET,
        ERROR_OCCURED_IN_PROCESS,
        ERROR_OCCURED_WHILE_DECOMPILING
    }

    interface OperationResultCreator {
        com.heliosdecompiler.helios.utils.Result create(Object... args);
    }

    class DefaultOperationResultCreator implements OperationResultCreator {
        private DefaultResult instance;

        DefaultOperationResultCreator(Type type) {
            this.instance = new DefaultResult(type);
        }

        @Override
        public com.heliosdecompiler.helios.utils.Result create(Object... args) {
            return instance;
        }
    }

    class InformationalOperationResultCreator implements OperationResultCreator {
        private Type type;

        InformationalOperationResultCreator(Type type) {
            this.type = type;
        }

        @Override
        public com.heliosdecompiler.helios.utils.Result create(Object... args) {
            return new InformationalResult(type, args);
        }
    }

    class DefaultResult implements com.heliosdecompiler.helios.utils.Result {
        private Type type;

        DefaultResult(Type type) {
            this.type = type;
        }

        public Type getType() {
            return this.type;
        }
    }

    class InformationalResult implements com.heliosdecompiler.helios.utils.Result {
        private Type type;

        private Object[] info;

        InformationalResult(Type type, Object... args) {
            this.type = type;
            this.info = args;
        }

        public Type getType() {
            return this.type;
        }

        public Object[] getInfo() {
            return this.info;
        }
    }
}
