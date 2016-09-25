package com.heliosdecompiler.helios.utils;

public interface Result {
    enum Type {
        SUCCESS,
        NO_SETTING_SPECIFIED,
        ERROR_OCCURED,
        INVALID_SETTING,
        NO_PYTHON2_SET,
        ERROR_OCCURED_IN_PROCESS,
        ERROR_OCCURED_WHILE_DECOMPILING
    }

    /**
     * Success!
     */
    OperationResultCreator SUCCESS = new DefaultOperationResultCreator(Type.SUCCESS);
    /**
     * The setting was empty or null
     * <p>
     * Used in {@link SettingsValidator}
     */
    OperationResultCreator NO_SETTING_SPECIFIED = new DefaultOperationResultCreator(Type.NO_SETTING_SPECIFIED);
    /**
     * The setting could not be validated due to some error
     * <p>
     * info[0] = Throwable
     *
     * Used in {@link SettingsValidator}
     */
    OperationResultCreator ERROR_OCCURED = new InformationalOperationResultCreator(Type.ERROR_OCCURED);
    /**
     * The setting was invalid
     * <p>
     * Used in {@link SettingsValidator}
     */
    OperationResultCreator INVALID_SETTING = new DefaultOperationResultCreator(Type.INVALID_SETTING);
    /**
     * Python 2 was not set
     */
    OperationResultCreator NO_PYTHON2_SET = new DefaultOperationResultCreator(Type.NO_PYTHON2_SET);
    /**
     * Could not run the process
     *
     * info[0] = Throwable
     * info[1] = String (process log)
     */
    OperationResultCreator ERROR_OCCURED_IN_PROCESS = new InformationalOperationResultCreator(Type.ERROR_OCCURED_IN_PROCESS);
    /**
     * Could not decompile
     *
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
