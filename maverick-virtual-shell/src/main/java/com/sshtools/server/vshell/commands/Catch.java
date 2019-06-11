/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.vshell.commands;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.Msh;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;

public class Catch extends Msh {

	public Catch() {
		super("catch", ShellCommand.SUBSYSTEM_SHELL, "<command> [<arg1>,<arg2>,..]",
				null,
				new Option("x", false, "Display full exception"),
				new Option("t", false,
						"Rethrow exception so shell / script exits"));
		setDescription("Run a command, catching exceptions it might throw");
		setBuiltIn(true);
	}

	public void run(CommandLine cli, VirtualProcess process)
			throws IOException, PermissionDeniedException {
		
		setCommandFactory(process.getMsh().getCommandFactory());
		
		String[] args = cli.getArgs();
		if (args.length < 2) {
			throw new IllegalArgumentException(
					"Expects at least a command name as an argument.");
		} else {
			try {
				String[] pArgs = new String[args.length - 1];
				System.arraycopy(args, 1, pArgs, 0, pArgs.length);
				doSpawn(process.getConsole(), process, pArgs, false);
			} catch (Exception e) {
				e.printStackTrace();
				if (cli.hasOption('x')) {
					process.getConsole().printException(e);
				} else {
					process.getConsole().printStringNewline(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
				}
				if (cli.hasOption('t')) {
					throw new IOException("An error occured. " + e.getMessage());
				}
			}
		}
	}

}