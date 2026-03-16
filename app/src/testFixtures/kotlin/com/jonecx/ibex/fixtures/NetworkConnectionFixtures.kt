package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.model.NetworkConnection
import com.jonecx.ibex.data.model.NetworkProtocol

object NetworkConnectionFixtures {

    val smbConnection = NetworkConnection(
        id = "smb-1",
        protocol = NetworkProtocol.SMB,
        displayName = "Office NAS",
        host = "192.168.1.100",
        username = "admin",
    )

    val ftpConnection = NetworkConnection(
        id = "ftp-1",
        protocol = NetworkProtocol.FTP,
        displayName = "FTP Server",
        host = "192.168.1.200",
        username = "ftpuser",
    )

    val cloudConnection = NetworkConnection(
        id = "cloud-1",
        protocol = NetworkProtocol.CLOUD,
        displayName = "My Cloud",
        host = "cloud.example.com",
        username = "clouduser",
    )

    val anonymousConnection = NetworkConnection(
        id = "anon-1",
        protocol = NetworkProtocol.SMB,
        displayName = "Public Share",
        host = "192.168.1.50",
        anonymous = true,
    )
}
