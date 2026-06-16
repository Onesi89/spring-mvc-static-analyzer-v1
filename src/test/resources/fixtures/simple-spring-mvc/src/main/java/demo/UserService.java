package demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    private final HistoryService historyService;

    public UserService(HistoryService historyService) {
        this.historyService = historyService;
    }

    public void validate() {
        userRepository.findByEmail();
    }

    public void createUser() {
        userRepository.save();
        historyService.saveHistory();
        externalClient.send();
    }
}
