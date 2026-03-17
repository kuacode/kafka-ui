import React, { useMemo } from 'react';
import { Cluster, ResourceType, ServerStatus } from 'generated-sources';
import { CellContext } from '@tanstack/react-table';
import { clusterConfigPath } from 'lib/paths';
import { useGetUserInfo } from 'lib/hooks/api/roles';
import { ActionCanButton } from 'components/common/ActionComponent';
import { useConnectCluster, useDisconnectCluster } from 'lib/hooks/api/clusters';
import { Button } from 'components/common/Button/Button';

type Props = CellContext<Cluster, unknown>;

const ClusterTableActionsCell: React.FC<Props> = ({ row }) => {
  const { name, status } = row.original;
  const { data } = useGetUserInfo();
  const connectCluster = useConnectCluster();
  const disconnectCluster = useDisconnectCluster();

  const hasPermissions = useMemo(() => {
    if (!data?.rbacEnabled) return true;
    return !!data?.userInfo?.permissions.some(
      (permission) => permission.resource === ResourceType.APPLICATIONCONFIG
    );
  }, [data]);

  const isOnline = status === ServerStatus.ONLINE;

  const handleConnect = () => {
    connectCluster.mutate(name);
  };

  const handleDisconnect = () => {
    disconnectCluster.mutate(name);
  };

  return (
    <div style={{ display: 'flex', gap: '8px' }}>
      {isOnline ? (
        <Button
          buttonType="secondary"
          buttonSize="S"
          onClick={handleDisconnect}
          disabled={disconnectCluster.isPending}
        >
          {disconnectCluster.isPending ? 'Disconnecting...' : 'Disconnect'}
        </Button>
      ) : (
        <Button
          buttonType="primary"
          buttonSize="S"
          onClick={handleConnect}
          disabled={connectCluster.isPending}
        >
          {connectCluster.isPending ? 'Connecting...' : 'Connect'}
        </Button>
      )}
      {hasPermissions && (
        <ActionCanButton
          buttonType="secondary"
          buttonSize="S"
          to={clusterConfigPath(name)}
          canDoAction={hasPermissions}
        >
          Configure
        </ActionCanButton>
      )}
    </div>
  );
};

export default ClusterTableActionsCell;
