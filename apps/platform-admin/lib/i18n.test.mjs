import test from 'node:test';
import assert from 'node:assert/strict';
import {preferredLanguage,translate} from './i18n.ts';
test('explicit safe locale overrides browser preference',()=>{assert.equal(preferredLanguage('en','fr-CM'),'en');assert.equal(preferredLanguage('fr','en'),'fr');assert.equal(preferredLanguage('../','fr-CM'),'fr');});
test('browser language respects preference quality and exclusions',()=>{assert.equal(preferredLanguage(undefined,'en;q=0.4, fr-CM;q=0.9'),'fr');assert.equal(preferredLanguage(undefined,'fr;q=0,en;q=1'),'en');assert.equal(preferredLanguage(undefined,'de,fr;q=no'),'en');});
test('French workflow and failures translate without changing amounts or references',()=>{assert.equal(translate('fr','Issue credit note'),'Émettre l’avoir');assert.equal(translate('fr','9223372036854775807 XAF'),'9223372036854775807 XAF');assert.equal(translate('fr','Check the submitted values. Reference: abc'),'Vérifiez les valeurs envoyées. Référence : abc');assert.match(translate('fr','reason: enter a value within 2000 characters.'),/2000 caractères/);assert.equal(translate('en','Issue credit note'),'Issue credit note');});
