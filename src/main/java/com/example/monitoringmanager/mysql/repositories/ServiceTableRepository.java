package com.example.monitoringmanager.mysql.repositories;

import com.example.monitoringmanager.mysql.tables.ServiceTable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ServiceTableRepository extends CrudRepository<ServiceTable, Integer> {
    ServiceTable findFirstByQueueName(String queueName);

    ServiceTable findFirstByServiceIp(String serviceIp);

    List<ServiceTable> findAll();
}
