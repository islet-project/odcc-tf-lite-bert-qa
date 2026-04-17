package org.tensorflow.lite.examples.bertqa.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.SavedStateHandle;
import androidx.navigation.NavArgs;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class QaFragmentArgs implements NavArgs {
  private final HashMap arguments = new HashMap();

  private QaFragmentArgs() {
  }

  @SuppressWarnings("unchecked")
  private QaFragmentArgs(HashMap argumentsMap) {
    this.arguments.putAll(argumentsMap);
  }

  @NonNull
  @SuppressWarnings("unchecked")
  public static QaFragmentArgs fromBundle(@NonNull Bundle bundle) {
    QaFragmentArgs __result = new QaFragmentArgs();
    bundle.setClassLoader(QaFragmentArgs.class.getClassLoader());
    if (bundle.containsKey("datasetPosition")) {
      int datasetPosition;
      datasetPosition = bundle.getInt("datasetPosition");
      __result.arguments.put("datasetPosition", datasetPosition);
    } else {
      throw new IllegalArgumentException("Required argument \"datasetPosition\" is missing and does not have an android:defaultValue");
    }
    return __result;
  }

  @NonNull
  @SuppressWarnings("unchecked")
  public static QaFragmentArgs fromSavedStateHandle(@NonNull SavedStateHandle savedStateHandle) {
    QaFragmentArgs __result = new QaFragmentArgs();
    if (savedStateHandle.contains("datasetPosition")) {
      int datasetPosition;
      datasetPosition = savedStateHandle.get("datasetPosition");
      __result.arguments.put("datasetPosition", datasetPosition);
    } else {
      throw new IllegalArgumentException("Required argument \"datasetPosition\" is missing and does not have an android:defaultValue");
    }
    return __result;
  }

  @SuppressWarnings("unchecked")
  public int getDatasetPosition() {
    return (int) arguments.get("datasetPosition");
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public Bundle toBundle() {
    Bundle __result = new Bundle();
    if (arguments.containsKey("datasetPosition")) {
      int datasetPosition = (int) arguments.get("datasetPosition");
      __result.putInt("datasetPosition", datasetPosition);
    }
    return __result;
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public SavedStateHandle toSavedStateHandle() {
    SavedStateHandle __result = new SavedStateHandle();
    if (arguments.containsKey("datasetPosition")) {
      int datasetPosition = (int) arguments.get("datasetPosition");
      __result.set("datasetPosition", datasetPosition);
    }
    return __result;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
        return true;
    }
    if (object == null || getClass() != object.getClass()) {
        return false;
    }
    QaFragmentArgs that = (QaFragmentArgs) object;
    if (arguments.containsKey("datasetPosition") != that.arguments.containsKey("datasetPosition")) {
      return false;
    }
    if (getDatasetPosition() != that.getDatasetPosition()) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 * result + getDatasetPosition();
    return result;
  }

  @Override
  public String toString() {
    return "QaFragmentArgs{"
        + "datasetPosition=" + getDatasetPosition()
        + "}";
  }

  public static final class Builder {
    private final HashMap arguments = new HashMap();

    @SuppressWarnings("unchecked")
    public Builder(@NonNull QaFragmentArgs original) {
      this.arguments.putAll(original.arguments);
    }

    @SuppressWarnings("unchecked")
    public Builder(int datasetPosition) {
      this.arguments.put("datasetPosition", datasetPosition);
    }

    @NonNull
    public QaFragmentArgs build() {
      QaFragmentArgs result = new QaFragmentArgs(arguments);
      return result;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public Builder setDatasetPosition(int datasetPosition) {
      this.arguments.put("datasetPosition", datasetPosition);
      return this;
    }

    @SuppressWarnings({"unchecked","GetterOnBuilder"})
    public int getDatasetPosition() {
      return (int) arguments.get("datasetPosition");
    }
  }
}
