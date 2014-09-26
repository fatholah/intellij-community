/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.components.store;

import com.intellij.openapi.components.StateStorage;
import com.intellij.openapi.components.impl.stores.StateStorageManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Set;

public interface ComponentSaveSession {
  @NotNull
  ComponentSaveSession save(@NotNull List<Pair<StateStorageManager.SaveSession, VirtualFile>> readonlyFiles);

  void finishSave();

  void reset();

  @Nullable
  Set<String> analyzeExternalChanges(@NotNull Set<Pair<VirtualFile, StateStorage>> changedFiles);

  @NotNull
  List<File> getAllStorageFiles(boolean includingSubStructures);
}