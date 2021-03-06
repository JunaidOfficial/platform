<script lang="ts">
    import {Component, Vue} from "vue-property-decorator";
    import BigNumber from "bignumber.js";
    import QrcodeVue from "qrcode.vue";

    import {Github, GithubIssue} from "../../classes/github";
    import {TokenInfo} from "../../classes/token-info";
    import {Contracts} from "../../classes/contracts";
    import {PaymentMethod, PaymentMethods} from "../../classes/payment-method";
    import {Web3x} from "../../classes/Web3x";
    import {Utils} from "../../classes/Utils";
    import {PendingFundCommand} from "../models/PendingFundCommand";
    import {Alert} from "../../classes/alert";

    Vue.component("qrcode-vue", QrcodeVue);

    @Component
    export default class WizardComponent extends Vue {
        private _activeStep: number = 1;
        private _loading: boolean = false;
        private _network: string;

        public githubUrl: string = "";
        public githubIssue: GithubIssue = null;
        public supportedTokens: TokenInfo[] = [];
        public selectedToken: TokenInfo = null;
        public panelsHeight: number = 0;
        public stepTitlesHeight: number = 0;
        public trustWalletModalActive: boolean = false;
        public approveInfoModalActive: boolean = false;
        public qrData: string = "";

        public currentAllowance: number = 0;
        public currentFundAmount: number = 0;

        public paymentMethod: PaymentMethod = PaymentMethods.getInstance().trustWallet;
        public fundAmount: number = 100;
        public description: string = "";

        mounted() {
            this.githubUrl = (this.$refs.requestUrl as HTMLElement).dataset.value;
            if (this.githubUrl) {
                this.updateUrl(this.githubUrl);
            }
            let metaNetwork = document.head.querySelector("[name=\"ethereum:network\"]");
            this._network = metaNetwork ? metaNetwork.getAttribute("content") : "";
            this.updateDappPaymentMethod();
            this.gotoStep(1);
        }

        public getClassesPanel(step: number) {
            if (this._activeStep == step) {
                return "movingIn";
            } else if (this._activeStep > step) {
                return "movingOutBackward";
            } else if (this._activeStep < step) {
                return "movingOutForward";
            } else {
                return "";
            }
        }

        public getClassesStep(step: number) {
            if (this._activeStep == step) {
                return "-active";
            } else if (this._activeStep > step) {
                return "-completed";
            } else {
                return "";
            }
        }

        public async gotoStep(step: number, onlyCompleted: boolean = false) {
            this._loading = true;
            let valid = true;
            if (step > this._activeStep && !onlyCompleted) {
                let $el: HTMLElement = <HTMLElement> this.$refs[`panelStep${this._activeStep}`];
                let formElements: HTMLElement[] = Array.from($el.querySelectorAll("[data-form-validation]"));
                for (let fieldElement of formElements) {
                    let validations: string[] = fieldElement.dataset.formValidation.split(";");
                    valid = await Utils.validateHTMLElement(fieldElement, validations) && valid;
                }
            }

            if (valid) {
                if (onlyCompleted && step < this._activeStep || !onlyCompleted) {
                    this._activeStep = step;
                    this.panelsHeight = (<HTMLElement>this.$refs[`panelStep${step}`]).clientHeight;
                    this.stepTitlesHeight = (<HTMLElement>this.$refs[`stepTitle${step}`]).clientHeight;
                }
            }
            this._loading = false;
        }

        private async updateDappPaymentMethod() {
            await this.updateDappDisabledMsg();
            if (!PaymentMethods.getInstance().dapp.disabledMsg) {
                this.paymentMethod = PaymentMethods.getInstance().dapp;
            } else {
                this.paymentMethod = PaymentMethods.getInstance().trustWallet;
            }
        }

        private async updateDappDisabledMsg() {
            let web3 = Web3x.getInstance();
            if (web3 && web3.eth && web3.eth.defaultAccount) {
                await new Promise((resolve, reject) => {
                    web3.version.getNetwork((err, res) => {
                        if (!err && res != this._network) {
                            PaymentMethods.getInstance().dapp.disabledMsg = "Not connected to main network.";
                        }
                        resolve("not connected");
                    });
                });
            } else {
                PaymentMethods.getInstance().dapp.disabledMsg = "DApp not available, no accounts available.";
            }
        }

        private async fund() {
            switch (this.paymentMethod) {
                case PaymentMethods.getInstance().dapp:
                    try {
                        if (await this.fundUsingDapp()) {
                            window.location.href = "/user/requests";
                        }
                    } catch(err) {
                        Alert.error(`Something went wrong during funding, please try again. <br/> If the problem remains, <a href="https://help.fundrequest.io">please contact the FundRequest team</a>.`);
                    }
                    break;
                case PaymentMethods.getInstance().trustWallet:
                    this.fundUsingTrustWallet();
                    break;
                default:
                    break;
            }
        }

        private _handleTransactionError(err): void {
            Utils.hideLoading();
            throw new Error(err);
        }

        private async fundUsingDapp(): Promise<boolean> {
            Utils.showLoading();
            let erc20 = await Contracts.getInstance().getErc20Contract(this.selectedToken.address);
            let _web3 = Web3x.getInstance();
            let account = _web3.eth.defaultAccount;
            let frContractAddress = Contracts.getInstance().frContractAddress;
            let decimals = (await erc20.decimals).toNumber();
            this.currentAllowance = (await erc20.allowance(account, frContractAddress)).toNumber();
            this.currentFundAmount = Number(this.fundAmount * Math.pow(10, decimals));
            Utils.hideLoading();

            if (!this.approveInfoModalActive && this.currentAllowance < this.currentFundAmount) {
                this.showApproveInfoModal();
                return false;
            }
            else {
                Utils.showLoading();
                if (this.currentAllowance > 0 && this.currentAllowance < this.currentFundAmount) {
                    // setting to 0
                    await erc20.approveTx(frContractAddress, 0).send({}).catch(rej => this._handleTransactionError(rej));
                    this.currentAllowance = 0;
                }
                if (this.currentAllowance === 0) {
                    // You will need to allow the FundRequest contract to access this token
                    await erc20.approveTx(frContractAddress, new BigNumber("1.157920892e77").minus(1)).send({}).catch(rej => this._handleTransactionError(rej));
                }
                let response = await (await Contracts.getInstance().getFrContract()).fundTx(_web3.fromAscii("GITHUB"), this.githubIssue.platformId, this.selectedToken.address, this.currentFundAmount)
                    .send({}).catch(rej => this._handleTransactionError(rej)) as string;
                let pendingFundCommand = new PendingFundCommand();
                pendingFundCommand.transactionId = response;
                pendingFundCommand.amount = this.fundAmount.toString();
                pendingFundCommand.description = this.description;
                pendingFundCommand.fromAddress = account;
                pendingFundCommand.tokenAddress = this.selectedToken.address;
                pendingFundCommand.platform = this.githubIssue.platform;
                pendingFundCommand.platformId = this.githubIssue.platformId;
                await Utils.postJSON(`/rest/pending-fund`, pendingFundCommand);
                Utils.hideLoading();
                return true;
            }
        }

        public fundUsingTrustWallet() {
            this.showTrustWalletModal();
        }

        public async showTrustWalletModal() {
            this.qrData = (await Utils.post(`/rest/requests/erc67/fund`, {
                platform: this.githubIssue.platform,
                platformId: this.githubIssue.platformId,
                amount: this.totalAmount,
                fundrequestAddress: Contracts.getInstance().frContractAddress,
                tokenAddress: Contracts.getInstance().tokenContractAddress
            })).erc67Link;

            Utils.modal.open(<HTMLElement>this.$refs.trustWalletModal, () => {
                this.hideTrustWalletModal();
            });

            this.trustWalletModalActive = true;
            this._fadeoutPage();
        }

        public showApproveInfoModal() {
            Utils.modal.open(<HTMLElement>this.$refs.approveInfoModal, () => {
                this.hideApproveInfoModal();
            });
            this.approveInfoModalActive = true;
            this._fadeoutPage();
        }

        public hideTrustWalletModal() {
            Utils.modal.close(<HTMLElement>this.$refs.trustWalletModal);
            this.trustWalletModalActive = false;
            this._fadeinPage();
        }

        public hideApproveInfoModal() {
            Utils.modal.close(<HTMLElement>this.$refs.approveInfoModal);
            this.approveInfoModalActive = false;
            this._fadeinPage();
        }

        public get totalAmount() {
            return this.fundAmount;
        }

        public get paymentMethods(): PaymentMethods {
            return PaymentMethods.getInstance();
        }

        public async updateUrl(url): Promise<void> {
            this.githubIssue = await Github.getGithubInfo(url);
            this.githubUrl = url;
            this.$forceUpdate();
            await this.updatePossibleTokens(this.githubIssue);
        }

        private _fadeoutPage() {
            (<HTMLElement>this.$refs.panels).style.opacity = "0.5";
            (<HTMLElement>this.$refs.panels).style.pointerEvents = "none";
            (<HTMLElement>this.$refs.faq).style.opacity = "0.5";
            (<HTMLElement>this.$refs.faq).style.pointerEvents = "none";
        }

        private _fadeinPage() {
            (<HTMLElement>this.$refs.panels).style.opacity = "";
            (<HTMLElement>this.$refs.panels).style.pointerEvents = "";
            (<HTMLElement>this.$refs.faq).style.opacity = "";
            (<HTMLElement>this.$refs.faq).style.pointerEvents = "";
        }

        private async updatePossibleTokens(res: GithubIssue): Promise<void> {
            if (res != null) {
                let tokens: TokenInfo[] = await Contracts.getPossibleTokens(res.platformId);
                if (tokens) {
                    this.supportedTokens = tokens;
                    this.selectedToken = this.supportedTokens[0];
                    this.$forceUpdate();
                }
            }
        }
    }
    /*
      public updateQr() {
        this.requestService.requestQRValue(new FundRequestCommand(
          this.requestDetails.platform,
          this.requestDetails.platformId,
          this.fundAmount
        )).then(
          res => { // Success
            this.qrValue = res;
          },
          msg => { // Error
          }
        ).catch();
      }

        return await this._http.post(ApiUrls.qrFund, body, {responseType: 'text'}).toPromise();

    let body = {
          platform: command.platform,
          platformId: command.platformId,
          amount: '' + this._web3.toWei(command.amount, 'ether'),
          fundrequestAddress: this._cs.getFundRequestContractAddress(),
          tokenAddress: this._cs.getTokenContractAddress()
        };
        return await this._http.post(ApiUrls.qrFund, body, {responseType: 'text'}).toPromise();

                                            <img class="img-responsive" [src]="'assets/img/qr-code.svg'"  />

     */
</script>
