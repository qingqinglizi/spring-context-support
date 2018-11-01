package org.springframework.scheduling.quartz;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.quartz.SchedulerConfigException;
import org.quartz.impl.jdbcjobstore.JobStoreCMT;
import org.quartz.impl.jdbcjobstore.SimpleSemaphore;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

public class LocalDataSourceJobStore
        extends JobStoreCMT
{
    public static final String TX_DATA_SOURCE_PREFIX = "springTxDataSource.";
    public static final String NON_TX_DATA_SOURCE_PREFIX = "springNonTxDataSource.";
    private DataSource dataSource;

    public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler)
            throws SchedulerConfigException
    {
        this.dataSource = SchedulerFactoryBean.getConfigTimeDataSource();
        if (this.dataSource == null) {
            throw new SchedulerConfigException("No local DataSource found for configuration - 'dataSource' property must be set on SchedulerFactoryBean");
        }
        setDataSource("springTxDataSource." + getInstanceName());
        setDontSetAutoCommitFalse(true);


        DBConnectionManager.getInstance().addConnectionProvider("springTxDataSource." +
                getInstanceName(), new ConnectionProvider()
        {
            public Connection getConnection()
                    throws SQLException
            {
                return DataSourceUtils.doGetConnection(LocalDataSourceJobStore.this.dataSource);
            }

            public void shutdown() {}

            public void initialize() {}
        });
        DataSource nonTxDataSource = SchedulerFactoryBean.getConfigTimeNonTransactionalDataSource();
        final DataSource nonTxDataSourceToUse = nonTxDataSource != null ? nonTxDataSource : this.dataSource;


        setNonManagedTXDataSource("springNonTxDataSource." + getInstanceName());


        DBConnectionManager.getInstance().addConnectionProvider("springNonTxDataSource." +
                getInstanceName(), new ConnectionProvider()
        {
            public Connection getConnection()
                    throws SQLException
            {
                return nonTxDataSourceToUse.getConnection();
            }

            public void shutdown() {}

            public void initialize() {}
        });
        try
        {
            String productName = JdbcUtils.extractDatabaseMetaData(this.dataSource, "getDatabaseProductName").toString();
            productName = JdbcUtils.commonDatabaseName(productName);
            if ((productName != null) && (productName.toLowerCase().contains("hsql")))
            {
                setUseDBLocks(false);
                setLockHandler(new SimpleSemaphore());
            }
        }
        catch (MetaDataAccessException ex)
        {
            logWarnIfNonZero(1, "Could not detect database type. Assuming locks can be taken.");
        }
        super.initialize(loadHelper, signaler);
    }

    protected void closeConnection(Connection con)
    {
        DataSourceUtils.releaseConnection(con, this.dataSource);
    }
}
