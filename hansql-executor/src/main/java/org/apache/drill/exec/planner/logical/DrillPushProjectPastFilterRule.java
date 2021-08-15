/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.planner.logical;

import org.lealone.hansql.optimizer.plan.RelOptRule;
import org.lealone.hansql.optimizer.rel.logical.LogicalFilter;
import org.lealone.hansql.optimizer.rel.logical.LogicalProject;
import org.lealone.hansql.optimizer.rel.rules.ProjectFilterTransposeRule;
import org.lealone.hansql.optimizer.rel.rules.PushProjector;

public class DrillPushProjectPastFilterRule extends ProjectFilterTransposeRule {

  public final static RelOptRule INSTANCE = new DrillPushProjectPastFilterRule(DrillConditions.PRESERVE_ITEM);

  protected DrillPushProjectPastFilterRule(PushProjector.ExprCondition preserveExprCondition) {
    super(LogicalProject.class, LogicalFilter.class,
        DrillRelFactories.LOGICAL_BUILDER, preserveExprCondition);
  }
}
