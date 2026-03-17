import { clustersApiClient as api } from 'lib/api';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ClusterName } from 'redux/interfaces';

export function useClusters() {
  return useQuery(['clusters'], () => api.getClusters(), { suspense: false });
}

export function useConnectCluster() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (clusterName: string) => api.connectCluster({ clusterName }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clusters'] });
    },
  });
}

export function useDisconnectCluster() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (clusterName: string) => api.disconnectCluster({ clusterName }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clusters'] });
    },
  });
}

export function useClusterStats(clusterName: ClusterName) {
  return useQuery(
    ['clusterStats', clusterName],
    () => api.getClusterStats({ clusterName }),
    { refetchInterval: 5000 }
  );
}
