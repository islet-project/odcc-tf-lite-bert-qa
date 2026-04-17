package org.tensorflow.lite.examples.bertqa.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.navigation.NavDirections;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;
import org.tensorflow.lite.examples.bertqa.R;

public class DatasetFragmentDirections {
  private DatasetFragmentDirections() {
  }

  @NonNull
  public static ActionDatasetFragmentToQaFragment actionDatasetFragmentToQaFragment(
      int datasetPosition) {
    return new ActionDatasetFragmentToQaFragment(datasetPosition);
  }

  public static class ActionDatasetFragmentToQaFragment implements NavDirections {
    private final HashMap arguments = new HashMap();

    @SuppressWarnings("unchecked")
    private ActionDatasetFragmentToQaFragment(int datasetPosition) {
      this.arguments.put("datasetPosition", datasetPosition);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public ActionDatasetFragmentToQaFragment setDatasetPosition(int datasetPosition) {
      this.arguments.put("datasetPosition", datasetPosition);
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    @NonNull
    public Bundle getArguments() {
      Bundle __result = new Bundle();
      if (arguments.containsKey("datasetPosition")) {
        int datasetPosition = (int) arguments.get("datasetPosition");
        __result.putInt("datasetPosition", datasetPosition);
      }
      return __result;
    }

    @Override
    public int getActionId() {
      return R.id.action_datasetFragment_to_qaFragment;
    }

    @SuppressWarnings("unchecked")
    public int getDatasetPosition() {
      return (int) arguments.get("datasetPosition");
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
          return true;
      }
      if (object == null || getClass() != object.getClass()) {
          return false;
      }
      ActionDatasetFragmentToQaFragment that = (ActionDatasetFragmentToQaFragment) object;
      if (arguments.containsKey("datasetPosition") != that.arguments.containsKey("datasetPosition")) {
        return false;
      }
      if (getDatasetPosition() != that.getDatasetPosition()) {
        return false;
      }
      if (getActionId() != that.getActionId()) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + getDatasetPosition();
      result = 31 * result + getActionId();
      return result;
    }

    @Override
    public String toString() {
      return "ActionDatasetFragmentToQaFragment(actionId=" + getActionId() + "){"
          + "datasetPosition=" + getDatasetPosition()
          + "}";
    }
  }
}
