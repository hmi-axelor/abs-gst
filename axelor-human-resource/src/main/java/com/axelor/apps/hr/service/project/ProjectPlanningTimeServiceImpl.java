/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.hr.service.project;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.base.service.weeklyplanning.WeeklyPlanningService;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.service.publicHoliday.PublicHolidayHrService;
import com.axelor.apps.project.db.Project;
import com.axelor.apps.project.db.ProjectPlanningTime;
import com.axelor.apps.project.db.repo.ProjectPlanningTimeRepository;
import com.axelor.apps.project.db.repo.ProjectRepository;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.exception.AxelorException;
import com.axelor.team.db.TeamTask;
import com.axelor.team.db.repo.TeamTaskRepository;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectPlanningTimeServiceImpl implements ProjectPlanningTimeService {

  private static final Logger log = LoggerFactory.getLogger(ProjectPlanningTimeService.class);

  @Inject private ProjectPlanningTimeRepository planningTimeRepo;

  @Inject private ProjectRepository projectRepo;

  @Inject private TeamTaskRepository teamTaskRepo;

  @Inject private WeeklyPlanningService weeklyPlanningService;

  @Inject private PublicHolidayHrService holidayService;

  @Inject private ProductRepository productRepo;

  @Inject private UserRepository userRepo;

  @Override
  public BigDecimal getTaskPlannedHrs(TeamTask task) {

    BigDecimal totalPlanned = BigDecimal.ZERO;
    if (task != null) {
      List<ProjectPlanningTime> plannings =
          planningTimeRepo
              .all()
              .filter(
                  "self.task = ?1 AND self.typeSelect = ?2",
                  task,
                  ProjectPlanningTimeRepository.TYPE_PROJECT_PLANNING_TIME)
              .fetch();
      if (plannings != null) {
        totalPlanned =
            plannings
                .stream()
                .map(p -> p.getPlannedHours())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
      }
    }

    return totalPlanned;
  }

  @Override
  public BigDecimal getTaskRealHrs(TeamTask task) {

    BigDecimal totalRealHrs = BigDecimal.ZERO;
    if (task != null) {
      List<ProjectPlanningTime> plannings =
          planningTimeRepo
              .all()
              .filter(
                  "self.task = ?1 AND self.typeSelect = ?2",
                  task,
                  ProjectPlanningTimeRepository.TYPE_PROJECT_PLANNING_TIME_SPENT)
              .fetch();
      if (plannings != null) {
        totalRealHrs =
            plannings.stream().map(p -> p.getRealHours()).reduce(BigDecimal.ZERO, BigDecimal::add);
      }
    }

    return totalRealHrs;
  }

  @Override
  public BigDecimal getProjectPlannedHrs(Project project) {

    BigDecimal totalPlanned = BigDecimal.ZERO;
    if (project != null) {
      List<ProjectPlanningTime> plannings =
          planningTimeRepo
              .all()
              .filter(
                  "(self.project = ?1 OR (self.project.parentProject = ?1 AND self.project.parentProject.isShowPhasesElements = ?2)) AND self.typeSelect = ?3",
                  project,
                  true,
                  ProjectPlanningTimeRepository.TYPE_PROJECT_PLANNING_TIME)
              .fetch();
      if (plannings != null) {
        totalPlanned =
            plannings
                .stream()
                .map(p -> p.getPlannedHours())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
      }
    }

    return totalPlanned;
  }

  @Override
  public BigDecimal getProjectRealHrs(Project project) {

    BigDecimal totalRealHrs = BigDecimal.ZERO;
    if (project != null) {
      List<ProjectPlanningTime> plannings =
          planningTimeRepo
              .all()
              .filter(
                  "(self.project = ?1 OR (self.project.parentProject = ?1 AND self.project.parentProject.isShowPhasesElements = ?2)) AND self.typeSelect = ?3",
                  project,
                  true,
                  ProjectPlanningTimeRepository.TYPE_PROJECT_PLANNING_TIME_SPENT)
              .fetch();
      if (plannings != null) {
        totalRealHrs =
            plannings.stream().map(p -> p.getRealHours()).reduce(BigDecimal.ZERO, BigDecimal::add);
      }
    }

    return totalRealHrs;
  }

  @Override
  @Transactional(rollbackOn = {Exception.class})
  public void addMultipleProjectPlanningTime(Map<String, Object> datas) throws AxelorException {

    if (datas.get("project") == null
        || datas.get("user") == null
        || datas.get("fromDate") == null
        || datas.get("toDate") == null) {
      return;
    }

    boolean isTimeSpent = false;
    if (datas.get("_timeSpent") != null) {
      isTimeSpent = (boolean) datas.get("_timeSpent");
    }
    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    LocalDateTime fromDate = LocalDateTime.parse(datas.get("fromDate").toString(), formatter);
    LocalDateTime toDate = LocalDateTime.parse(datas.get("toDate").toString(), formatter);

    TeamTask teamTask = null;

    Map<String, Object> objMap = (Map) datas.get("project");
    Project project = projectRepo.find(Long.parseLong(objMap.get("id").toString()));
    Integer timePercent = 0;

    if (datas.get("timepercent") != null) {
      timePercent = Integer.parseInt(datas.get("timepercent").toString());
    }

    objMap = (Map) datas.get("user");
    User user = userRepo.find(Long.parseLong(objMap.get("id").toString()));

    if (user.getEmployee() == null) {
      return;
    }

    if (datas.get("task") != null) {
      objMap = (Map) datas.get("task");
      teamTask = teamTaskRepo.find(Long.valueOf(objMap.get("id").toString()));
    }

    Product activity = null;
    if (datas.get("product") != null) {
      objMap = (Map) datas.get("product");
      activity = productRepo.find(Long.valueOf(objMap.get("id").toString()));
    }

    Employee employee = user.getEmployee();
    BigDecimal dailyWorkHrs = employee.getDailyWorkHours();

    while (fromDate.isBefore(toDate)) {

      LocalDate date = fromDate.toLocalDate();

      log.debug("Create Planning for the date: {}", date);

      double dayHrs = 0;
      if (employee.getWeeklyPlanning() != null) {
        dayHrs = weeklyPlanningService.getWorkingDayValueInDays(employee.getWeeklyPlanning(), date);
      }

      if (dayHrs > 0 && !holidayService.checkPublicHolidayDay(date, employee)) {

        ProjectPlanningTime planningTime = new ProjectPlanningTime();

        planningTime.setTask(teamTask);
        planningTime.setProduct(activity);
        planningTime.setTimepercent(timePercent);
        planningTime.setUser(user);
        planningTime.setDate(date);
        planningTime.setProject(project);
        planningTime.setIsIncludeInTurnoverForecast(
            (Boolean) datas.get("isIncludeInTurnoverForecast"));
        planningTime.setTypeSelect((Integer) datas.get("_typeSelect"));

        BigDecimal totalHours = BigDecimal.ZERO;
        if (timePercent > 0) {
          totalHours =
              dailyWorkHrs.multiply(new BigDecimal(timePercent)).divide(new BigDecimal(100));
        }
        if (isTimeSpent) {
          planningTime.setRealHours(totalHours);
        } else {
          planningTime.setPlannedHours(totalHours);
        }
        planningTimeRepo.save(planningTime);
      }

      fromDate = fromDate.plusDays(1);
    }
  }

  @Override
  @Transactional
  public void removeProjectPlanningLines(List<Map<String, Object>> projectPlanningLines) {

    for (Map<String, Object> line : projectPlanningLines) {
      ProjectPlanningTime projectPlanningTime =
          planningTimeRepo.find(Long.parseLong(line.get("id").toString()));
      planningTimeRepo.remove(projectPlanningTime);
    }
  }
}
