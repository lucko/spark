/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.standalone.remote;

import me.lucko.spark.standalone.StandaloneSparkPlugin;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.jline.builtins.ssh.ShellFactoryImpl;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.logging.Level;

public class SshRemoteInterface extends AbstractRemoteInterface {
    private final String password;
    private final SshServer sshd;

    public SshRemoteInterface(StandaloneSparkPlugin spark, int port) {
        super(spark);
        this.password = new SecureRandom().ints(48, 122)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(32)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        this.sshd = SshServer.setUpDefaultServer();
        if (port > 0) {
            this.sshd.setPort(port);
        }
        this.sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        this.sshd.setPasswordAuthenticator((username, password, session) -> "spark".equals(username) && MessageDigest.isEqual(this.password.getBytes(), password.getBytes()));
        this.sshd.setShellFactory(new ShellFactoryImpl(shellParams -> this.processSession(shellParams.getTerminal(), shellParams.getCloser())));

        try {
            this.start();
        } catch (IOException e) {
            this.spark.log(Level.SEVERE, "Error whilst starting SSH server", e);
        }
    }

    private void start() throws IOException {
        this.sshd.start();
        this.spark.log(Level.INFO, "SSH Server started on port " + this.sshd.getPort());
        this.spark.log(Level.INFO, "Connect using: ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -p " + this.sshd.getPort() + " spark@localhost");
        this.spark.log(Level.INFO, "When prompted, enter the password: " + this.password);
    }

    public String getPassword() {
        return this.password;
    }

    public int getPort() {
        return this.sshd.getPort();
    }

    @Override
    public void close() {
        try {
            this.sshd.stop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}