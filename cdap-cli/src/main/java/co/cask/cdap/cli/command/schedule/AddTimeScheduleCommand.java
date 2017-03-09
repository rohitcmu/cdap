/*
 * Copyright © 2015-2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package co.cask.cdap.cli.command.schedule;

import co.cask.cdap.api.schedule.Schedule;
import co.cask.cdap.api.schedule.Schedules;
import co.cask.cdap.cli.ArgumentName;
import co.cask.cdap.cli.CLIConfig;
import co.cask.cdap.cli.ElementType;
import co.cask.cdap.cli.english.Article;
import co.cask.cdap.cli.english.Fragment;
import co.cask.cdap.cli.exception.CommandInputError;
import co.cask.cdap.cli.util.AbstractCommand;
import co.cask.cdap.cli.util.ArgumentParser;
import co.cask.cdap.client.ScheduleClient;
import co.cask.cdap.proto.ScheduleInstanceConfiguration;
import co.cask.cdap.proto.id.ScheduleId;
import co.cask.common.cli.Arguments;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Map;

/**
 * Adds a schedule.
 */
public final class AddTimeScheduleCommand extends AbstractCommand {

  private final ScheduleClient scheduleClient;

  @Inject
  public AddTimeScheduleCommand(CLIConfig cliConfig, ScheduleClient scheduleClient) {
    super(cliConfig);
    this.scheduleClient = scheduleClient;
  }

  @Override
  public void perform(Arguments arguments, PrintStream printStream) throws Exception {
    String[] scheduleIdParts = arguments.get(ArgumentName.SCHEDULE.toString()).split("\\.");
    String programType = arguments.get(ArgumentName.PROGRAM_TYPE.toString()).toUpperCase();
    String[] programIdParts = arguments.get(ArgumentName.PROGRAM.toString()).split("\\.");
    String scheduleDescription = arguments.getOptional(ArgumentName.DESCRIPTION.toString(), "");
    String cronExpression = arguments.get(ArgumentName.CRON_EXPRESSION.toString());
    String schedulePropertiesString = arguments.getOptional(ArgumentName.SCHEDULE_PROPERTIES.toString(), "");
    String scheduleRunConstraintsString = arguments.getOptional(ArgumentName.SCHEDULE_RUN_CONSTRAINTS.toString(), "");

    if (scheduleIdParts.length < 2 || programIdParts.length < 2 || !scheduleIdParts[0].equals(programIdParts[0])) {
      throw new CommandInputError(this);
    }

    String appId = scheduleIdParts[0];
    String scheduleName = scheduleIdParts[1];
    ScheduleId scheduleId = cliConfig.getCurrentNamespace().app(appId).schedule(scheduleName);

    Map<String, String> constraintsMap =
      ArgumentParser.parseMap(scheduleRunConstraintsString, ArgumentName.SCHEDULE_RUN_CONSTRAINTS.toString());

    Schedules.Builder builder = Schedules.builder(scheduleName);
    if (constraintsMap.containsKey("maxConcurrentRuns")) {
      builder.setMaxConcurrentRuns(Integer.valueOf("maxConcurrentRuns"));
    }
    if (scheduleDescription != null) {
      builder.setDescription(scheduleDescription);
    }
    Schedule schedule = builder.createTimeSchedule(cronExpression);

    Map<String, String> programMap = ImmutableMap.of("programName", programIdParts[1],
                                                     "programType", programType);
    Map<String, String> propertiesMap = ArgumentParser.parseMap(schedulePropertiesString,
                                                                ArgumentName.SCHEDULE_PROPERTIES.toString());

    ScheduleInstanceConfiguration configuration =
      new ScheduleInstanceConfiguration("TIME", schedule, programMap, propertiesMap);

    scheduleClient.add(scheduleId, configuration);
    printStream.printf("Successfully added schedule '%s' in program '%s'\n", scheduleName, appId);
  }

  @Override
  public String getPattern() {
    return String.format("add time schedule <%s> for <%s> <%s> " +
                           "[description <%s>] at <%s> [properties <%s>] [constraints <%s>]",
                         ArgumentName.SCHEDULE, ArgumentName.PROGRAM_TYPE, ArgumentName.PROGRAM,
                         ArgumentName.DESCRIPTION, ArgumentName.CRON_EXPRESSION, ArgumentName.SCHEDULE_PROPERTIES,
                         ArgumentName.SCHEDULE_RUN_CONSTRAINTS);
  }

  @Override
  public String getDescription() {
    return String.format("Adds %s which is associated with %s given.",
                         Fragment.of(Article.A, ElementType.SCHEDULE.getName()),
                         Fragment.of(Article.THE, ElementType.PROGRAM.getName()));
  }
}
