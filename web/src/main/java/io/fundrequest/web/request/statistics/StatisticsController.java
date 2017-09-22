package io.fundrequest.web.request.statistics;

import io.fundrequest.core.request.RequestService;
import io.fundrequest.core.request.fund.FundService;
import io.fundrequest.core.request.fund.dto.FundDto;
import io.fundrequest.core.request.view.RequestDto;
import io.fundrequest.web.request.statistics.dto.StatisticsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class StatisticsController {

    private RequestService requestService;
    private FundService fundService;

    @Autowired
    public StatisticsController(RequestService requestService, FundService fundService) {
        this.requestService = requestService;
        this.fundService = fundService;
    }

    @GetMapping("/requests/statistics")
    public StatisticsDto getStatistics() {
        StatisticsDto result = new StatisticsDto();
        List<RequestDto> allRequests = requestService.findAll();
        List<FundDto> funds = fundService.findAll();
        result.setNumberOfIssues((long) allRequests.size());
        result.setNumberOfFunders(
                funds.stream().map(FundDto::getFunder).distinct().count()
        );
        result.setTotalAmountFunded(
                funds.stream().mapToLong(FundDto::getAmountInWei).sum()
        );
        long numberOfRequestsFunded = funds.stream().map(FundDto::getRequestId).distinct().count();
        if (numberOfRequestsFunded > 0) {
            result.setPercentageFunded((double) numberOfRequestsFunded / (double) result.getNumberOfIssues() * 100);
            result.setAverageFundingPerIssue((double) result.getTotalAmountFunded() / (numberOfRequestsFunded));
        } else {
            result.setPercentageFunded(0d);
            result.setAverageFundingPerIssue(0d);
        }

        return result;
    }
}