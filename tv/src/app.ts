/** Easy Xtream Football for Samsung TV: boot, strings, first screen. */
import './polyfills';
import strings from '../www/strings.json';
import { initStrings } from './core/i18n';
import { installRemoteLog } from './core/rlog';
import { probeBandwidthIfStale } from './core/bandwidth';
import { Store } from './core/storage';
import { Repository } from './core/repository';
import { Profile } from './core/model';
import { installKeyboard } from './ui/focus';
import { reset, push } from './ui/screens';
import { profilesScreen } from './ui/profiles';
import { addProfileScreen } from './ui/addProfile';
import { channelsScreen } from './ui/channels';

function openProfile(p: Profile): void {
  Store.setLastProfileId(p.id);
  Repository.bindProfile(p);
  push(channelsScreen());
}

function home(): void {
  const profiles = Store.profiles();
  const screen = profilesScreen(openProfile);
  reset(screen);
  if (profiles.length === 0) {
    push(addProfileScreen(null, (p) => { reset(profilesScreen(openProfile)); openProfile(p); }));
  } else {
    const last = Store.lastProfileId();
    const p = last !== null ? profiles.find((x) => x.id === last) : undefined;
    if (p) openProfile(p);
  }
}

installRemoteLog();
initStrings(strings as any);
installKeyboard();
(window as any).__exfRerender = home;
home();
probeBandwidthIfStale();
