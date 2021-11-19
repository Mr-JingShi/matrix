/*
 * Tencent is pleased to support the open source community by making wechat-matrix available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.matrix;

import android.app.Application;

import com.tencent.matrix.lifecycle.owners.MultiProcessLifecycleInitializer;
import com.tencent.matrix.lifecycle.supervisor.ProcessSupervisor;
import com.tencent.matrix.lifecycle.supervisor.SupervisorConfig;
import com.tencent.matrix.plugin.DefaultPluginListener;
import com.tencent.matrix.plugin.Plugin;
import com.tencent.matrix.plugin.PluginListener;
import com.tencent.matrix.util.MatrixLog;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Created by zhangshaowen on 17/5/17.
 */

public class Matrix {
    private static final String TAG = "Matrix.Matrix";


    private static volatile Matrix sInstance;

    private final HashSet<Plugin> plugins;
    private final Application application;
    private final PluginListener pluginListener;

    private Matrix(Application app, PluginListener listener, HashSet<Plugin> plugins, SupervisorConfig supervisorConfig, List<String> baseActivities) {
        this.application = app;
        this.pluginListener = listener;
        this.plugins = plugins;
        MultiProcessLifecycleInitializer.init(app, baseActivities);
        ProcessSupervisor.INSTANCE.init(app, supervisorConfig);
        for (Plugin plugin : plugins) {
            plugin.init(application, pluginListener);
            pluginListener.onInit(plugin);
        }

    }

    public static void setLogIml(MatrixLog.MatrixLogImp imp) {
        MatrixLog.setMatrixLogImp(imp);
    }

    public static boolean isInstalled() {
        return sInstance != null;
    }

    public static Matrix init(Matrix matrix) {
        if (matrix == null) {
            throw new RuntimeException("Matrix init, Matrix should not be null.");
        }
        synchronized (Matrix.class) {
            if (sInstance == null) {
                sInstance = matrix;
            } else {
                MatrixLog.e(TAG, "Matrix instance is already set. this invoking will be ignored");
            }
        }
        return sInstance;
    }

    public static Matrix with() {
        if (sInstance == null) {
            throw new RuntimeException("you must init Matrix sdk first");
        }
        return sInstance;
    }

    public void startAllPlugins() {
        for (Plugin plugin : plugins) {
            plugin.start();
        }
    }

    public void stopAllPlugins() {
        for (Plugin plugin : plugins) {
            plugin.stop();
        }
    }

    public void destroyAllPlugins() {
        for (Plugin plugin : plugins) {
            plugin.destroy();
        }
    }

    public Application getApplication() {
        return application;
    }

    public HashSet<Plugin> getPlugins() {
        return plugins;
    }

    public Plugin getPluginByTag(String tag) {
        for (Plugin plugin : plugins) {
            if (plugin.getTag().equals(tag)) {
                return plugin;
            }
        }
        return null;
    }

    public <T extends Plugin> T getPluginByClass(Class<T> pluginClass) {
        String className = pluginClass.getName();
        for (Plugin plugin : plugins) {
            if (plugin.getClass().getName().equals(className)) {
                return (T) plugin;
            }
        }
        return null;
    }

    public static class Builder {
        private final Application application;
        private PluginListener pluginListener;
        private SupervisorConfig mSupervisorConfig;
        private List<String> baseActivities = Collections.emptyList();

        private HashSet<Plugin> plugins = new HashSet<>();

        public Builder(Application app) {
            if (app == null) {
                throw new RuntimeException("matrix init, application is null");
            }
            this.application = app;
        }

        public Builder plugin(Plugin plugin) {
            String tag = plugin.getTag();
            for (Plugin exist : plugins) {
                if (tag.equals(exist.getTag())) {
                    throw new RuntimeException(String.format("plugin with tag %s is already exist", tag));
                }
            }
            plugins.add(plugin);
            return this;
        }

        public Builder pluginListener(PluginListener pluginListener) {
            this.pluginListener = pluginListener;
            return this;
        }

        /**
         * see {@link SupervisorConfig}
         * @param config
         * @return
         */
        @Deprecated
        public Builder supervisorConfig(SupervisorConfig config) {
            this.mSupervisorConfig = config;
            return this;
        }

        public Builder baseActivities(List<String> baseActivities) {
            this.baseActivities = baseActivities;
            return this;
        }

        public Matrix build() {
            if (pluginListener == null) {
                pluginListener = new DefaultPluginListener(application);
            }
            return new Matrix(application, pluginListener, plugins, mSupervisorConfig, baseActivities);
        }

    }
}
