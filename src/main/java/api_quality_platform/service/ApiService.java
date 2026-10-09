package api_quality_platform.service;

import api_quality_platform.entity.Api;
import api_quality_platform.repository.ApiRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiService {

    private final ApiRepository apiRepository;

    public ApiService(ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    public Api createApi(Api api) {
        return apiRepository.save(api);
    }

    public List<Api> getAllApis() {
        return apiRepository.findAll();
    }
}
