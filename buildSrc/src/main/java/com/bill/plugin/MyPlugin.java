package com.bill.plugin;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.BaseExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Created by Bill on 2022/5/3.
 */

class MyPlugin implements Plugin<Project> {

    @Override
    public void apply(Project target) {

        AppExtension extension = target.getExtensions().findByType(AppExtension.class);
        if (extension != null) {
            extension.registerTransform(new MyTransform());
        }

    }
}
