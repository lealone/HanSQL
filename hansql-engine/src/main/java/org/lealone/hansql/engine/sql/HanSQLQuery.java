/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lealone.hansql.engine.sql;

import java.util.ArrayList;

import org.lealone.db.async.AsyncHandler;
import org.lealone.db.async.AsyncResult;
import org.lealone.db.result.Result;
import org.lealone.db.result.ResultTarget;
import org.lealone.db.session.ServerSession;
import org.lealone.db.session.SessionStatus;
import org.lealone.hansql.engine.HanEngine;
import org.lealone.hansql.engine.server.HanClientConnection;
import org.lealone.hansql.exec.SqlExecutor;
import org.lealone.hansql.optimizer.schema.SchemaPlus;
import org.lealone.net.NetNode;
import org.lealone.sql.SQLStatement;
import org.lealone.sql.StatementBase;
import org.lealone.sql.executor.YieldableBase;
import org.lealone.sql.query.YieldableQueryBase;

public class HanSQLQuery extends StatementBase {

    private final String sql;
    private boolean useDefaultSchema;

    public HanSQLQuery(ServerSession session, String sql, boolean useDefaultSchema) {
        super(session);
        this.sql = sql;
        this.useDefaultSchema = useDefaultSchema;
        parameters = new ArrayList<>();
    }

    @Override
    public int getType() {
        return SQLStatement.SELECT;
    }

    @Override
    public boolean isQuery() {
        return true;
    }

    @Override
    public Result query(int maxRows) {
        YieldableHanSQLQuery yieldable = new YieldableHanSQLQuery(this, maxRows, false, null, null);
        return syncExecute(yieldable);
    }

    @Override
    public YieldableBase<Result> createYieldableQuery(int maxRows, boolean scrollable,
            AsyncHandler<AsyncResult<Result>> asyncHandler) {
        return new YieldableHanSQLQuery(this, maxRows, scrollable, asyncHandler, null);
    }

    private static class YieldableHanSQLQuery extends YieldableQueryBase {

        private final HanSQLQuery select;
        // private final ResultTarget target;
        private Result result;

        public YieldableHanSQLQuery(HanSQLQuery select, int maxRows, boolean scrollable,
                AsyncHandler<AsyncResult<Result>> asyncHandler, ResultTarget target) {
            super(select, maxRows, scrollable, asyncHandler);
            this.select = select;
            // this.target = target;
        }

        @Override
        protected boolean startInternal() {
            return false;
        }

        @Override
        protected void stopInternal() {
        }

        @Override
        protected void executeInternal() {
            if (result == null && this.pendingException == null) {
                session.setStatus(SessionStatus.STATEMENT_RUNNING);
                executeQueryAsync(select.getSession(), select.sql, true);
            }
        }

        private void executeQueryAsync(ServerSession session, String sql, boolean useDefaultSchema) {
            HanEngine hanEngine = HanEngine.getInstance();
            SchemaPlus rootSchema = hanEngine.getRootSchema(session, sql, useDefaultSchema, false);
            HanClientConnection clientConnection = new HanClientConnection(rootSchema,
                    select.useDefaultSchema, session, hanEngine,
                    NetNode.getLocalTcpNode().getInetSocketAddress(), null, res -> {
                        if (res.isSucceeded()) {
                            result = res.getResult();
                            setResult(result, result.getRowCount());
                        } else {
                            setPendingException(res.getCause());
                        }
                        session.setStatus(SessionStatus.STATEMENT_COMPLETED);
                        session.getTransactionListener().wakeUp();
                    });
            // hanEngine.submitWork(clientConnection, sql);
            SqlExecutor sqlExecutor = hanEngine.createSqlExecutor(clientConnection, sql);
            sqlExecutor.run(false);
        }
    }
}
