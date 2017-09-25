/* global Vue */
require(['jquery-easypiechart','jquery','pubsub'], function() {
    'use strict';

    let $ = requirejs('jquery');
    let pubsub = requirejs('pubsub');

    let ComponentStat = {
        props: ['icon', 'val', 'text'],
        template: '#component-stat'
    };

    let ComponentFunded = {
        props: ['val', 'text'],
        template: '#component-funded'
    };

    // create a root instance
    let v_dashboard = new Vue({
        el: '#dashboard',
        components: {
            'component-stat': ComponentStat,
            'component-funded': ComponentFunded
        },
        data: {
            numberOfRequests: {
                icon: 'fa fa-file-code-o',
                text: '# Requests',
                val: 0
            },
            numberOfFunders: {
                icon: 'icon-people',
                text: '# Funders',
                val: 0
            },
            totalAmountFunded: {
                icon: 'icon-fire',
                text: 'Total funded',
                val: 0
            },
            averageFundingPerRequest: {
                icon: 'icon-chart',
                text: 'Average funding',
                val: 0
            },
            percentageFunded: {
                text: 'Funded requests',
                val: 0
            },
        },
        methods: {
            fromWei(amountInWei) {
                var number = Number(amountInWei) / 1000000000000000000;
                return ((Math.round(number * 100) / 100).toFixed(2)).toLocaleString();
            },
            round(amount, digitsAfterDecimal) {
                if (typeof digitsAfterDecimal === 'undefined') {
                    digitsAfterDecimal = 2;
                }
                var number = Number(amount);
                return ((Math.round(number * 100) / 100).toFixed(digitsAfterDecimal)).toLocaleString()
            }
        }
    });

    pubsub.subscribe('fnd/stats/update', function() {
        $.get('/requests/statistics', function(data) {
            v_dashboard.numberOfRequests.val = data.numberOfRequests;
            v_dashboard.numberOfFunders.val = data.numberOfFunders;
            v_dashboard.totalAmountFunded.val = data.totalAmountFunded;
            v_dashboard.averageFundingPerRequest.val = data.averageFundingPerRequest;
            v_dashboard.percentageFunded.val = data.percentageFunded;

            $('#dashboard').find('[data-easypiechart]').each(function() {
                let $this = $(this);
                $this.easyPieChart();
            });
        });
    });

    pubsub.publish('fnd/stats/update');
});