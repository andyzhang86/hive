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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hadoop.hive.metastore.columnstats.merge;

import org.apache.hadoop.hive.common.ndv.NumDistinctValueEstimator;
import org.apache.hadoop.hive.metastore.api.ColumnStatisticsObj;
import org.apache.hadoop.hive.metastore.columnstats.cache.StringColumnStatsDataInspector;

import static org.apache.hadoop.hive.metastore.columnstats.ColumnsStatsUtils.stringInspectorFromStats;

public class StringColumnStatsMerger extends ColumnStatsMerger {
  @Override
  protected void doMerge(ColumnStatisticsObj aggregateColStats, ColumnStatisticsObj newColStats) {
    StringColumnStatsDataInspector aggregateData = stringInspectorFromStats(aggregateColStats);
    StringColumnStatsDataInspector newData = stringInspectorFromStats(newColStats);
    aggregateData.setMaxColLen(Math.max(aggregateData.getMaxColLen(), newData.getMaxColLen()));
    aggregateData.setAvgColLen(Math.max(aggregateData.getAvgColLen(), newData.getAvgColLen()));
    aggregateData.setNumNulls(aggregateData.getNumNulls() + newData.getNumNulls());
    if (aggregateData.getNdvEstimator() == null || newData.getNdvEstimator() == null) {
      aggregateData.setNumDVs(Math.max(aggregateData.getNumDVs(), newData.getNumDVs()));
    } else {
      NumDistinctValueEstimator oldEst = aggregateData.getNdvEstimator();
      NumDistinctValueEstimator newEst = newData.getNdvEstimator();
      final long ndv;
      if (oldEst.canMerge(newEst)) {
        oldEst.mergeEstimators(newEst);
        ndv = oldEst.estimateNumDistinctValues();
        aggregateData.setNdvEstimator(oldEst);
      } else {
        ndv = Math.max(aggregateData.getNumDVs(), newData.getNumDVs());
      }
      log.debug("Use bitvector to merge column {}'s ndvs of {} and {} to be {}", aggregateColStats.getColName(),
          aggregateData.getNumDVs(), newData.getNumDVs(), ndv);
      aggregateData.setNumDVs(ndv);
    }

    aggregateColStats.getStatsData().setStringStats(aggregateData);
  }
}
