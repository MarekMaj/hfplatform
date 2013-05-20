package com.marekmaj.hfplatform.event;


import com.marekmaj.hfplatform.service.AccountService;

public interface AccountCommand {

    public Result execute(final AccountService accountService);
}
