package com.marekmaj.hfplatform.event.incoming;


import com.marekmaj.hfplatform.service.AccountService;

public interface AccountCommand {

    public void execute(final AccountService accountService);
}
