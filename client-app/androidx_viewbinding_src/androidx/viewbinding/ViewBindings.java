/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.viewbinding;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

/**
 * Internal utilities for the generated code.
 *
 * @hide
 */
public class ViewBindings {

    private ViewBindings() {
    }

    /**
     * Like `findViewById` but skips the view itself.
     *
     * @hide
     */
    @Nullable
    public static <T extends View> T findChildViewById(View rootView, @IdRes int id) {
        if (!(rootView instanceof ViewGroup)) {
            return null;
        }
        final ViewGroup rootViewGroup = (ViewGroup) rootView;
        final int childCount = rootViewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final T view = rootViewGroup.getChildAt(i).findViewById(id);
            if (view != null) {
                return view;
            }
        }
        return null;
    }
}
