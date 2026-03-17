package com.provectus.kafka.ui.service;

import com.provectus.kafka.ui.mapper.ClusterMapper;
import com.provectus.kafka.ui.model.ClusterDTO;
import com.provectus.kafka.ui.model.ClusterMetricsDTO;
import com.provectus.kafka.ui.model.ClusterStatsDTO;
import com.provectus.kafka.ui.model.InternalClusterState;
import com.provectus.kafka.ui.model.KafkaCluster;
import com.provectus.kafka.ui.model.ServerStatusDTO;
import com.provectus.kafka.ui.model.Statistics;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClusterService {

  private final StatisticsCache statisticsCache;
  private final ClustersStorage clustersStorage;
  private final ClusterMapper clusterMapper;
  private final StatisticsService statisticsService;

  public List<ClusterDTO> getClusters() {
    return clustersStorage.getKafkaClusters()
        .stream()
        .map(c -> clusterMapper.toCluster(new InternalClusterState(c, statisticsCache.get(c))))
        .collect(Collectors.toList());
  }

  public Mono<ClusterStatsDTO> getClusterStats(KafkaCluster cluster) {
    return Mono.justOrEmpty(
        clusterMapper.toClusterStats(
            new InternalClusterState(cluster, statisticsCache.get(cluster)))
    );
  }

  public Mono<ClusterMetricsDTO> getClusterMetrics(KafkaCluster cluster) {

    return Mono.just(
        clusterMapper.toClusterMetrics(
            statisticsCache.get(cluster).getMetrics()));
  }

  public Mono<ClusterDTO> updateCluster(KafkaCluster cluster) {
    return statisticsService.updateCache(cluster)
        .map(metrics -> clusterMapper.toCluster(new InternalClusterState(cluster, metrics)));
  }

  public Mono<ClusterDTO> connectCluster(String clusterName) {
    return Mono.fromCallable(() -> clustersStorage.getClusterByName(clusterName))
        .flatMap(clusterOpt -> {
          if (clusterOpt.isEmpty()) {
            return Mono.empty();
          }
          KafkaCluster cluster = clusterOpt.get();
          cluster.getOriginalProperties().setEnabled(true);
          var offline = Statistics.empty().toBuilder().status(ServerStatusDTO.INITIALIZING).build();
          statisticsCache.replace(cluster, offline);
          return statisticsService.updateCache(cluster)
              .map(metrics -> clusterMapper.toCluster(new InternalClusterState(cluster, metrics)));
        });
  }

  public Mono<ClusterDTO> disconnectCluster(String clusterName) {
    return Mono.fromCallable(() -> clustersStorage.getClusterByName(clusterName))
        .flatMap(clusterOpt -> {
          if (clusterOpt.isEmpty()) {
            return Mono.empty();
          }
          KafkaCluster cluster = clusterOpt.get();
          cluster.getOriginalProperties().setEnabled(false);
          var offline = Statistics.empty().toBuilder().status(ServerStatusDTO.OFFLINE).build();
          statisticsCache.replace(cluster, offline);
          return Mono.just(clusterMapper.toCluster(new InternalClusterState(cluster, offline)));
        });
  }
}