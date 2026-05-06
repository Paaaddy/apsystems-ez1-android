package com.apsystems.ez1monitor

import com.apsystems.ez1monitor.data.repository.DemoDataSource
import com.apsystems.ez1monitor.data.repository.EZ1DataSource
import com.apsystems.ez1monitor.data.repository.EZ1Repository

object DataSourceCreator {
    fun create(isDemoMode: Boolean): EZ1DataSource =
        if (isDemoMode) DemoDataSource() else EZ1Repository()
}
